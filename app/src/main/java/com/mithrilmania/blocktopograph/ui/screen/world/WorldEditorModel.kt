package com.mithrilmania.blocktopograph.ui.screen.world

import android.content.Intent
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.editor.world.SelectorPaneState
import com.mithrilmania.blocktopograph.map.MarkerManager
import com.mithrilmania.blocktopograph.map.Player
import com.mithrilmania.blocktopograph.map.dummyJob
import com.mithrilmania.blocktopograph.map.edit.EditResultCode
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.selection.Selection
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readAnonymousTypedTag
import com.mithrilmania.blocktopograph.registry.Registry
import com.mithrilmania.blocktopograph.ui.component.FloatingPanelScaffoldState
import com.mithrilmania.blocktopograph.ui.component.PanelValue
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.Signal
import com.mithrilmania.blocktopograph.util.math.DimensionVec3f
import com.mithrilmania.blocktopograph.util.startsWith
import com.mithrilmania.blocktopograph.world.CHUNK_DIMENSION
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.KeyPrefix
import com.mithrilmania.blocktopograph.world.LoadedWorld
import com.mithrilmania.blocktopograph.world.NO_CACHE_OPTION
import com.mithrilmania.blocktopograph.world.access.boxed
import com.mithrilmania.blocktopograph.world.access.extractPlayerPos
import com.mithrilmania.blocktopograph.world.access.resolveLocalPlayerPos
import com.mithrilmania.blocktopograph.world.chunk.ChunkCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.iq80.leveldb.DB
import kotlin.math.floor

const val ZOOM_LEVELS = 4

enum class MapLayer(@JvmField @field:StringRes val display: Int) {
    SATELLITE(R.string.satellite),
    SLIME_CHUNKS(R.string.slime_chunks),
    HEIGHT_MAP(R.string.heightmap)
}

class SelectionV2 : Selection() {
    override var isSelecting: Boolean by mutableStateOf(false)
}

class WorldEditorModel(
    @JvmField val world: LoadedWorld,
    dimension: Dimension,
    collapseOffset: (Density, Int, Boolean) -> Float
) : ViewModel() {
    @JvmField
    val blocks: Registry<BlockTemplate> = BlockTemplates.REGISTRY_FACTORY.create()

    @JvmField
    val chunks = ChunkCache(viewModelScope, world, blocks)

    @JvmField
    val intents = Channel<Intent>(Channel.CONFLATED)

    @JvmField
    val scaffoldState = FloatingPanelScaffoldState(
        initialValue = PanelValue.Peek,
        snackbarHostState = SnackbarHostState(),
        allowHiddenState = false,
        collapseOffset = collapseOffset
    )
    @JvmField
    val tabPager = PagerState(0, 0F) { 3 }

    @JvmField
    val selection = SelectionV2()
    var enabledLayer: MapLayer by mutableStateOf(MapLayer.SATELLITE)
    var dimension: Dimension by mutableStateOf(dimension)

    /**
     * Position of the latest map long-press while its options menu is visible.
     */
    var longPressPos by mutableStateOf<LongPressPos?>(null)

    /**
     * Pending request that drives the "create custom marker" dialog from the long-press menu.
     */
    var createMarkerRequest by mutableStateOf<CreateMarkerRequest?>(null)

    /**
     * Whether the coordinate grid overlay is shown on the map.
     */
    var showGrid by mutableStateOf(true)

    /**
     * Whether markers are shown on the map. Built-in markers are rendered by ChunkedMarkerLayer,
     * but this switch is not wired to that layer yet, so it currently only holds the intended
     * visibility state.
     */
    var showMarkers by mutableStateOf(true)

    /**
     * Result stream of selection operations, observed by [SelectionPanel].
     */
    @JvmField
    val editResult = Signal<EditResultCode>()

    /**
     * Whether a selection operation is currently running on the IO dispatcher.
     */
    var waitingJob by mutableStateOf(false)
        private set
    private var blockingJob: Job = dummyJob()

    /**
     * Tracks [job] as the current blocking selection operation and exposes its state
     * through [waitingJob]. A newer job cancels the previous one.
     */
    fun waitForJob(job: Job) {
        val current = this.blockingJob
        this.blockingJob = job
        this.waitingJob = true
        current.cancel()
        job.invokeOnCompletion {
            if (this.blockingJob === job) {
                this.waitingJob = false
            }
        }
    }

    /** Cancels the selection operation currently shown by the waiting indicator. */
    fun cancelBlockingJob() {
        this.blockingJob.cancel()
    }

    @JvmField
    val coordinateX = TextFieldState()

    @JvmField
    val coordinateZ = TextFieldState()

    /**
     * Manual-input state for the selection panel, mirroring the selector pane.
     */
    @JvmField
    val selectorPaneState = SelectorPaneState()

    /**
     * Opens the long-press options menu for the given map coordinate and derives its chunk.
     * Negative coordinates are floored to match the legacy map behavior.
     */
    fun onMapLongPress(worldX: Double, worldZ: Double) {
        this.longPressPos = LongPressPos(
            worldX,
            worldZ,
            floor(worldX / CHUNK_DIMENSION).toInt(),
            floor(worldZ / CHUNK_DIMENSION).toInt()
        )
    }

    /**
     * Creates and registers a custom marker in the world marker store. Returns a string
     * resource describing the failure, or `null` on success. Persistence is triggered
     * separately through [persistMarkers].
     */
    fun createCustomMarker(
        displayName: String,
        iconName: String,
        x: Int,
        y: Int,
        z: Int,
        dimension: Dimension
    ): Int? = try {
        val marker = MarkerManager.markerFromData(displayName, iconName, x, y, z, dimension)
        world.access.markers.addMarker(marker, true)
        null
    } catch (e: Exception) {
        Log.e(APP_TAG, "Failed to create marker", e)
        R.string.failed_to_create_marker
    }

    /**
     * Writes the marker store to disk when a custom marker was added.
     */
    fun persistMarkers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                world.access.markers.saveToFile()
            } catch (e: Exception) {
                Log.e(APP_TAG, "Failed to persist markers", e)
            }
        }
    }

    var players by mutableStateOf<List<Player>?>(null)
        private set
    private var playerLoader: Job = dummyJob()

    var markers by mutableStateOf<List<AbstractMarker>?>(null)
        private set
    private var markerLoader: Job = dummyJob()

    fun loadPlayers(db: DB): Job {
        var loader = this.playerLoader
        if (loader.isActive) return loader
        loader = viewModelScope.launch(Dispatchers.IO) {
            val players = mutableListOf<Player>()
            try {
                val localPlayerPos: DimensionVec3f? = world.access.resolveLocalPlayerPos(db)
                if (localPlayerPos !== null) {
                    players.add(Player.localPlayer().apply {
                        position = localPlayerPos.boxed()
                    })
                }
                db.iterator(NO_CACHE_OPTION)
                    .use { iterator ->
                        val prefix = KeyPrefix.ONLINE_PLAYER.bytes
                        iterator.seek(prefix)
                        while (iterator.hasNext()) {
                            val entry = iterator.next()
                            val key = entry.key
                            if (!key.startsWith(prefix)) break
                            val player = Player.networkPlayer(key.toString(Charsets.UTF_8))
                            players.add(player)
                            player.position = BedrockNBTInput(entry.value)
                                .readAnonymousTypedTag<CompoundTag>()
                                ?.extractPlayerPos()
                                ?.boxed()
                                ?: continue
                        }
                    }
            } catch (_: Exception) {
                // TODO log
            }
            withContext(Dispatchers.Main) {
                this@WorldEditorModel.players = players
            }
        }
        this.playerLoader = loader
        return loader
    }

    fun loadMarkers(): Job {
        var loader = this.markerLoader
        if (loader.isActive) return loader
        loader = viewModelScope.launch(Dispatchers.IO) {
            val markers: List<AbstractMarker> = try {
                world.access.markers.listMarkers()
            } catch (_: Exception) {
                // TODO log
                emptyList()
            }
            withContext(Dispatchers.Main) {
                this@WorldEditorModel.markers = markers
            }
        }
        this.markerLoader = loader
        return loader
    }

    override fun onCleared() {
        this.world.close()
    }
}
