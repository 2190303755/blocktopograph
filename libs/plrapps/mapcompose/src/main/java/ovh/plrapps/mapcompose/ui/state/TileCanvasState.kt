package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.core.ColorFilterProvider
import ovh.plrapps.mapcompose.core.LayerFactory
import ovh.plrapps.mapcompose.core.SpaceKey
import ovh.plrapps.mapcompose.core.Tile
import ovh.plrapps.mapcompose.core.TileCollector
import ovh.plrapps.mapcompose.core.TileSpec
import ovh.plrapps.mapcompose.core.Viewport
import ovh.plrapps.mapcompose.core.VisibleTiles
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.core.contains
import ovh.plrapps.mapcompose.core.debounce
import ovh.plrapps.mapcompose.core.intersects
import ovh.plrapps.mapcompose.core.spaceKey
import ovh.plrapps.mapcompose.core.throttle
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

/**
 * This class contains all the logic related to [Tile] management.
 * It defers [Tile] loading to the [TileCollector].
 * All internal data manipulation are thread-confined to a single background thread. This is
 * guarantied by the [scope] and its custom dispatcher.
 * Ultimately, it exposes the list of tiles to render ([tilesToRender]) which is backed by a
 * [MutableState]. A composable using [tilesToRender] will be automatically recomposed when this
 * list changes.
 *
 * @author P.Laurence on 04/06/2019
 */
internal class TileCanvasState(
    parentScope: CoroutineScope,
    private val visibleTilesResolver: VisibleTilesResolver,
    workerCount: Int
) {

    /* This view-model uses a background thread for its computations */
    private val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(
        parentScope.coroutineContext + singleThreadDispatcher
    )
    internal var tilesToRender: List<Tile> by mutableStateOf(listOf())
    private var tilesCollectedBySpace: Map<SpaceKey, Tile> = mapOf()

    private val _layerFlow = MutableStateFlow<LayerFactory?>(null)
    fun hasLayer(): Boolean = this._layerFlow.value !== null

    private val visibleTileLocationsChannel = Channel<TileSpec>(capacity = Channel.RENDEZVOUS)
    private val tilesOutput = Channel<Tile>(capacity = Channel.RENDEZVOUS)
    private val visibleStateFlow = MutableStateFlow<VisibleState?>(null)
    internal var alphaTick = 0.07f
        set(value) {
            field = value.coerceIn(0.01f, 1f)
        }
    internal var colorFilterProvider: ColorFilterProvider? by mutableStateOf(null)
    private val recycleChannel = Channel<Tile>(Channel.UNLIMITED)

    /**
     * So long as this debounced channel is offered a message, the lambda isn't called.
     */
    private val idleDebounced = scope.debounce<Unit>(400.milliseconds) {
        visibleStateFlow.value?.also { (visibleTiles, layerId) ->
            evictTiles(visibleTiles, layerId, aggressiveAttempt = true)
            renderTiles(visibleTiles, layerId)
        }
    }

    private val renderTask = scope.throttle(wait = 34.milliseconds) {
        /* Evict, then render */
        val state = visibleStateFlow.value
        if (state === null) {
            tilesCollected.clear()
            return@throttle
        }
        val (lastVisible, id) = state
        evictTiles(lastVisible, id)
        renderTiles(lastVisible, id)
    }

    private fun renderTiles(
        visibleTiles: VisibleTiles,
        layerId: String
    ) {
        /* Right before sending tiles to the view, reorder them so that tiles from current level are
         * above others. */
        val tilesToRenderCopy = tilesCollected.sortedBy {
            /* As a side effect of sorting tiles, also set tile phases */

            val priority =
                if (it.zoom == visibleTiles.level && it.subSample == visibleTiles.subSample) 100 else 0
            priority + if (it.layerId == layerId) 1 else 0
        }

        tilesToRender = tilesToRenderCopy
    }

    private val tilesCollected = mutableSetOf<Tile>()

    private val tileCollector: TileCollector

    init {
        /* Collect visible tiles and send specs to the TileCollector */
        scope.launch {
            collectNewTiles()
        }

        /* Launch the TileCollector */
        tileCollector = TileCollector(
            workerCount = workerCount.coerceAtLeast(1)
        )
        scope.launch {
            _layerFlow.collectLatest { layer ->
                if (layer !== null) {
                    tileCollector.collectTiles(
                        tileSpecs = visibleTileLocationsChannel,
                        tilesOutput = tilesOutput,
                        layer = layer
                    )
                }
            }
        }

        /* Launch a coroutine to consume the produced tiles */
        scope.launch {
            consumeTiles(tilesOutput)
        }

        /* This is very important to null a tile's bitmap on the main thread because this ensures
         * that on the next composition the bitmap won't be accessed.
         * In the future, if the Compose framework does multi-threaded rendering, another technique
         * will have to be used. Or, consider not using Bitmap.recycle() at all since it seems
         * not necessary for hardware bitmaps. */
        scope.launch(Dispatchers.Main) {
            for (t in recycleChannel) {
                val b = t.bitmap
                t.bitmap = null
                b?.recycle()
            }
        }
    }

    fun setLayer(layer: LayerFactory?) {
        _layerFlow.value = layer
    }

    /**
     * Forgets visible state and previously collected tiles.
     * To clear the canvas, call [forgetTiles], then [renderThrottled].
     */
    suspend fun forgetTiles() {
        scope.launch {
            visibleStateFlow.value = null
            tilesCollected.clear()
        }.join()
    }

    fun shutdown() {
        singleThreadDispatcher.close()
        tileCollector.shutdownNow()
    }

    suspend fun setViewport(viewport: Viewport) {
        /* Thread-confine the tileResolver to the main thread */
        val visibleTiles = withContext(Dispatchers.Main) {
            visibleTilesResolver.getVisibleTiles(viewport)
        }

        withContext(scope.coroutineContext) {
            setVisibleTiles(visibleTiles)
        }
    }

    private fun setVisibleTiles(visibleTiles: VisibleTiles) {
        /* Feed the tile processing machinery */
        val layer = _layerFlow.value

        visibleStateFlow.value = if (layer === null) {
            null
        } else {
            VisibleState(visibleTiles, layer.id)
        }

        renderThrottled()
    }

    /**
     * Consumes incoming visible tiles from [visibleStateFlow] and sends [TileSpec] instances to the
     * [TileCollector].
     *
     * Leverage built-in back pressure, as this function will suspend when the tile collector is busy
     * to the point it can't handshake the [visibleTileLocationsChannel] channel.
     *
     * Using [Flow.collectLatest], we cancel any ongoing previous tile list processing. It's
     * particularly useful when the [TileCollector] is too slow, so when a new [VisibleTiles] element
     * is received from [visibleStateFlow], no new [TileSpec] elements from the previous [VisibleTiles]
     * element are sent to the [TileCollector]. When the [TileCollector] is ready to resume processing,
     * the latest [VisibleTiles] element is processed right away.
     */
    private suspend fun collectNewTiles() {
        visibleStateFlow.collectLatest { visibleState ->
            if (visibleState != null) {
                sendSpecsForTileMatrix(visibleState)
            }
        }
    }

    private suspend fun sendSpecsForTileMatrix(
        visibleState: VisibleState
    ) {
        val visibleTiles = visibleState.visibleTiles
        val left = visibleTiles.left
        val right = visibleTiles.right
        for (row in visibleTiles.top..visibleTiles.bottom) {
            for (col in left..right) {
                val tile = Tile(
                    zoom = visibleTiles.level,
                    row = row,
                    col = col,
                    subSample = visibleTiles.subSample,
                    layerId = visibleState.layerId
                )
                val alreadyProcessed = tilesCollected.contains(tile)

                /* Only emit specs which haven't already been processed by the collector
                 * Doing this now results in less object allocations than filtering the flow
                 * afterwards */
                if (!alreadyProcessed) {
                    visibleTileLocationsChannel.send(
                        TileSpec(
                            visibleTiles.level,
                            row,
                            col,
                            visibleTiles.subSample
                        )
                    )
                }
            }
        }
    }

    /**
     * For each [Tile] received, add it to the list of collected tiles if it's visible. Otherwise,
     * recycle the tile.
     */
    private suspend fun consumeTiles(tileChannel: ReceiveChannel<Tile>) {
        for (tile in tileChannel) {
            val (lastVisible, layerId) = visibleStateFlow.value ?: continue

            if (
                lastVisible.contains(tile)
                && !tilesCollected.contains(tile)
                && tile.layerId == layerId
            ) {
                val tileWithSameSpace = tilesCollectedBySpace[tile.spaceKey()]
                if (tileWithSameSpace == null || tileWithSameSpace.layerId == layerId) {
                    tile.prepare()
                } else {
                    tile.overlaps = tileWithSameSpace
                    /* A tile already occupies the same space, so we don't need any fade-in */
                    tile.alpha = 1f
                }
                tilesCollected.add(tile)
                renderThrottled()
            } else {
                tile.recycle()
            }
            fullEvictionDebounced()
        }
    }

    private fun fullEvictionDebounced() {
        idleDebounced.tryEmit(Unit)
    }

    /**
     * The the alpha needs to be set to [alphaTick], to produce a fade-in effect. If [alphaTick] is
     * 1f, the alpha won't be updated and there won't be any fade-in effect.
     */
    private fun Tile.prepare() {
        alpha = alphaTick
    }

    private fun updateTileCollectedBySpace() {
        tilesCollectedBySpace = tilesCollected.associateBy {
            it.spaceKey()
        }
    }

    /**
     * Each time we get a new [VisibleTiles], remove all [Tile] from [tilesCollected] which aren't
     * visible or that aren't needed anymore and put their bitmap into the pool.
     */
    private fun evictTiles(
        visibleTiles: VisibleTiles,
        layerId: String,
        aggressiveAttempt: Boolean = false
    ) {
        val currentLevel = visibleTiles.level
        val currentSubSample = visibleTiles.subSample

        /* Always perform partial eviction */
        partialEviction(visibleTiles, layerId)

        /* Only perform aggressive eviction when tile collector is idle */
        if (aggressiveAttempt && tileCollector.isIdle) {
            aggressiveEviction(currentLevel, currentSubSample, layerId)
        }

        /* Now that tileCollected is cleaned up, update an internal data structure */
        updateTileCollectedBySpace()
    }

    /**
     * Evict:
     * * tiles of levels different than the current one, that aren't visible,
     * * tiles that aren't visible at current level, and tiles from current level which aren't made
     * of current layers
     */
    private fun partialEviction(
        visibleTiles: VisibleTiles,
        layerId: String
    ) {
        val currentLevel = visibleTiles.level
        val currentSubSample = visibleTiles.subSample
        val addedSet = mutableSetOf<SpaceKey>()

        val iterator = tilesCollected.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()

            if (tile.layerId == layerId) {
                val spaceHash = tile.spaceKey()
                addedSet.add(spaceHash)
            }

            if (tile.zoom != currentLevel && !visibleTiles.intersects(tile)) {
                iterator.remove()
                tile.recycle()
                continue
            }

            if (
                tile.zoom == currentLevel
                && tile.subSample == currentSubSample
                && (!visibleTiles.contains(tile) || tile.markedForSweep)
            ) {
                iterator.remove()
                tile.recycle()
            }
        }

        /* Now that we know all tiles with the latest layerIds, forget the other
         * tiles which occupy the same space. Don't recycle the associated bitmaps because some of
         * the latest tiles haven't been drawn yet. So we rely on garbage collection for these
         * bitmaps. */
        val secondPass = tilesCollected.iterator()
        while (secondPass.hasNext()) {
            val tile = secondPass.next()
            if (tile.layerId == layerId) continue
            val spaceHash = tile.spaceKey()
            if (addedSet.contains(spaceHash)) {
                secondPass.remove()
            }
        }
    }

    /**
     * Removes tiles of other levels, even if they are visible (although they should be drawn beneath
     * currently visible tiles).
     * Only triggered after the [idleDebounced] fires.
     */
    private fun aggressiveEviction(
        currentLevel: Int,
        currentSubSample: Int,
        layerId: String
    ) {
        val iterator = tilesCollected.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()

            /* Remove tiles at the same level but from other layers */
            if (
                tile.zoom == currentLevel
                && tile.subSample == currentSubSample
                && tile.layerId != layerId
            ) {
                iterator.remove()
                tile.recycle()
            }

            /* Remove other tiles at different level and sub-sample */
            if ((tile.zoom != currentLevel && tile.subSample == 0)
                || (tile.zoom == 0 && tile.subSample != currentSubSample)
            ) {
                iterator.remove()
                tile.recycle()
            }
        }
    }

    /**
     * Post a new value to the observable. The view should update its UI.
     */
    private fun renderThrottled() {
        renderTask.trySend(Unit)
    }

    /**
     * After a [Tile] is no longer visible, depending on the bitmap mutability:
     * - If the Bitmap is mutable, put it into the pool for later use.
     * - If the bitmap isn't mutable, we don't use bitmap pooling. That means the associated graphic
     * memory can be reclaimed asap.
     * The Compose framework draws tiles on the main thread and checks whether or not [Tile.bitmap]
     * is null. So, prior to calling recycle() we set [Tile.bitmap] to null on the main thread. This
     * is done inside the coroutine which consumes [recycleChannel].
     */
    private fun Tile.recycle() {
        val b = bitmap ?: return
        if (!b.isMutable) {
            recycleChannel.trySend(this)
        }
        alpha = 0f
    }

    private data class VisibleState(
        val visibleTiles: VisibleTiles,
        val layerId: String
    )
}
