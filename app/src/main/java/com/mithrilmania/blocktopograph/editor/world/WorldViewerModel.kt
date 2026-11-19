package com.mithrilmania.blocktopograph.editor.world

import android.app.Application
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.map.Player
import com.mithrilmania.blocktopograph.map.dummyJob
import com.mithrilmania.blocktopograph.map.edit.EditResultCode
import com.mithrilmania.blocktopograph.map.edit.SearchAndReplaceRequest
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_AREA
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_LENGTH
import com.mithrilmania.blocktopograph.map.picer.PicerState
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.map.selection.Selection
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.nbt.io.readAnonymousTypedTag
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.AppNavKey.NBTEditor
import com.mithrilmania.blocktopograph.ui.screen.nbt.ConfirmationRequest
import com.mithrilmania.blocktopograph.util.Signal
import com.mithrilmania.blocktopograph.util.math.DimensionVec3f
import com.mithrilmania.blocktopograph.util.startsWith
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.KeyPrefix
import com.mithrilmania.blocktopograph.world.NO_CACHE_OPTION
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.access.WorldAccess
import com.mithrilmania.blocktopograph.world.access.boxed
import com.mithrilmania.blocktopograph.world.access.extractPlayerPos
import com.mithrilmania.blocktopograph.world.access.resolveLocalPlayerPos
import com.mithrilmania.blocktopograph.world.defaultMapTypeCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.Camera
import kotlin.math.sqrt

class WorldViewerModel(app: Application) : AndroidViewModel(app) {
    @JvmField
    val backStack = mutableStateListOf<AppNavKey>(AppNavKey.WorldList)

    @JvmField
    val modified = mutableStateOf(false)

    @JvmField
    val confirmation = mutableStateOf<ConfirmationRequest?>(null)

    fun requestOrExecute(request: ConfirmationRequest) {
        if (this.modified.value) {
            this.confirmation.value = request
        } else {
            request.invoke()
        }
    }

    fun checkAndOpenNBTEditor(
        source: NBTSource,
        config: NBTImportConfig
    ) {
        requestOrExecute {
            val stack = backStack
            if (stack.size > 1) {
                stack[stack.lastIndex] = NBTEditor(source to config, crossfade = true)
            } else {
                stack.add(NBTEditor(source to config))
            }
        }
    }

    @JvmField
    val pendingMovement: Channel<Camera> = Channel(Channel.CONFLATED)

    @JvmField
    val pendingIntent: Channel<Intent> = Channel()

    @JvmField
    val pendingMarkers: Channel<List<AbstractMarker>> = Channel(capacity = 4)

    var dimension: Dimension = VanillaDimension.OVERWORLD
        private set

    @JvmField
    val mapType: MutableLiveData<MapType> =
        MutableLiveData<MapType>(dimension.defaultMapTypeCompat())

    @JvmField
    val showGrid: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)

    @JvmField
    val showMarkers: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)

    @JvmField
    val selection = object : Selection() {
        override var isSelecting: Boolean
            get() = paneType == PaneType.SELECTOR
            set(value) {
                if (value) {
                    paneType = PaneType.SELECTOR
                } else if (paneType == PaneType.SELECTOR) {
                    paneType = PaneType.NONE
                }
            }
    }
    @JvmField
    val showDrawer = Signal<Unit>()
    @JvmField
    val longPressCenter = Signal<Unit>()

    @JvmField
    val editResult = Signal<EditResultCode>()

    @JvmField
    val snackbar = SnackbarHostState()
    var picerState by mutableStateOf<PicerState?>(null)
    var replacingRequest by mutableStateOf<SearchAndReplaceRequest?>(null)
    var paneType by mutableStateOf(PaneType.NONE)

    @JvmField
    val selectorPaneState = SelectorPaneState()

    @JvmField
    val locatorPaneState = LocatorPaneState()
    var players by mutableStateOf<List<Player>?>(null)
    private var playerLoader: Job = dummyJob()
    var markers by mutableStateOf<List<AbstractMarker>?>(null)
    private var markerLoader: Job = dummyJob()
    private var blockingJob: Job = dummyJob()
    var waitingJob by mutableStateOf(false)
        private set
    var longPressPos by mutableStateOf<LongPressPos?>(null)
    var confirmCloseWorld by mutableStateOf(false)
    var showOpenByNameDialog by mutableStateOf(false)

    /**
     * Set when a marker on the map is tapped, to show the marker options dialog.
     */
    var tappedMarker by mutableStateOf<AbstractMarker?>(null)

    /**
     * One-time notice encouraging the user to try map selection.
     */
    var showTrySelectionNotice by mutableStateOf(false)

    /**
     * One-time notice shown when too many markers are rendered on screen.
     */
    var showTooManyMarkersNotice by mutableStateOf(false)

    var markerFilterVisible by mutableStateOf(false)

    var createMarkerRequest by mutableStateOf<CreateMarkerRequest?>(null)

    fun cancelBlockingJob() {
        this.blockingJob.cancel()
    }

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

    fun navigateTo(dimension: Dimension, type: MapType? = null) {
        this.dimension = dimension
        this.mapType.value = type ?: dimension.defaultMapTypeCompat()
    }

    fun commitAnalyzedState(rect: IntRect, fallback: PicerState) {
        val width = rect.width
        val height = rect.height
        if (width > 0 && height > 0) {
            val maxEdgeScale = PICER_MAX_LENGTH / maxOf(width, height)
            if (maxEdgeScale >= 1) {
                val maxAreaFactor = PICER_MAX_AREA / (width * height)
                val maxScale = if (maxEdgeScale * maxEdgeScale > maxAreaFactor)
                    sqrt(maxAreaFactor.toDouble()).toInt()
                else maxEdgeScale
                if (maxScale >= 1) {
                    this.picerState = PicerState.Analyzed(rect, maxScale)
                    return
                }
            }
        }
        this.picerState = fallback
    }

    fun loadPlayers(world: WorldAccess, storage: WorldStorage): Job {
        var loader = this.playerLoader
        if (loader.isActive) return loader
        loader = viewModelScope.launch(Dispatchers.IO) {
            val players = mutableListOf<Player>()
            try {
                val localPlayerPos: DimensionVec3f? = world.resolveLocalPlayerPos(storage)
                if (localPlayerPos !== null) {
                    players.add(Player.localPlayer().apply {
                        position = localPlayerPos.boxed()
                    })
                }
                storage.db?.iterator(NO_CACHE_OPTION)
                    ?.use { iterator ->
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
                this@WorldViewerModel.players = players
            }
        }
        this.playerLoader = loader
        return loader
    }

    fun loadMarkers(world: WorldAccess): Job {
        var loader = this.markerLoader
        if (loader.isActive) return loader
        loader = viewModelScope.launch(Dispatchers.IO) {
            val markers: List<AbstractMarker> = try {
                world.markers.listMarkers()
            } catch (_: Exception) {
                // TODO log
                emptyList()
            }
            withContext(Dispatchers.Main) {
                this@WorldViewerModel.markers = markers
            }
        }
        this.markerLoader = loader
        return loader
    }
}