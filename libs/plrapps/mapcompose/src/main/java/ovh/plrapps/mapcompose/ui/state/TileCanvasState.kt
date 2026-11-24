package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.core.ColorFilterProvider
import ovh.plrapps.mapcompose.core.LayerFactory
import ovh.plrapps.mapcompose.core.SpaceKey
import ovh.plrapps.mapcompose.core.Tile
import ovh.plrapps.mapcompose.core.TileCollector
import ovh.plrapps.mapcompose.core.TileSpec
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
 * Tile bookkeeping - collection, eviction and rendering - is thread-confined to a single background
 * thread. This is guaranteed by the [scope] and its custom dispatcher. The visible state is
 * published to [visibleStateFlow] from the caller's thread.
 * Ultimately, it exposes the list of tiles to render ([tilesToRender]) which is backed by a
 * [androidx.compose.runtime.MutableState]. A composable using [tilesToRender] will be automatically
 * recomposed when this list changes.
 *
 * @author P.Laurence on 04/06/2019
 */
internal class TileCanvasState(
    parentScope: CoroutineScope,
    private val visibleTilesResolver: VisibleTilesResolver,
    workerCount: Int
) : AutoCloseable {

    /* This view-model uses a background thread for its computations */
    private val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(
        parentScope.coroutineContext + singleThreadDispatcher
    )
    internal var tilesToRender: List<Tile> by mutableStateOf(emptyList())
        private set

    private var tilesCollectedBySpace: Map<SpaceKey, Tile> = emptyMap()

    internal val layer = MutableStateFlow<LayerFactory?>(null)

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

            val priority = if (it.zoom == visibleTiles.level) 100 else 0
            priority + if (it.layerId == layerId) 1 else 0
        }

        tilesToRender = tilesToRenderCopy
    }

    private val tilesCollected = hashSetOf<Tile>()

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
            layer.collectLatest {
                if (it !== null) {
                    tileCollector.collectTiles(
                        tileSpecs = visibleTileLocationsChannel,
                        tilesOutput = tilesOutput,
                        layer = it
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

    /**
     * Closes the single-thread dispatcher and interrupts the tile workers. Called by
     * MapModel.onCleared() only after `viewModelScope.cancel()` has run.
     */
    override fun close() {
        singleThreadDispatcher.close()
        tileCollector.shutdownNow()
    }

    /**
     * Resolves visible tiles from plain pixel bounds, then publishes the result to
     * [visibleStateFlow]. The resolver is immutable and thread-safe, so Main-thread execution is
     * not required for correctness (the current caller runs on `viewModelScope`).
     */
    fun setViewport(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        scale: Double
    ) {
        setVisibleTiles(
            visibleTilesResolver.getVisibleTiles(left, top, right, bottom, scale)
        )
    }

    private fun setVisibleTiles(visibleTiles: VisibleTiles) {
        /* Feed the tile processing machinery */
        val layer = layer.value

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
     * Using [kotlinx.coroutines.flow.Flow.collectLatest], we cancel any ongoing previous tile list processing. It's
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
                    layerId = visibleState.layerId
                )
                val alreadyProcessed = tilesCollected.contains(tile)

                /* Only emit specs which haven't already been processed by the collector
                 * Doing this now results in less object allocations than filtering the flow
                 * afterwards */
                if (!alreadyProcessed) {
                    visibleTileLocationsChannel.send(
                        TileSpec(visibleTiles.level, row, col)
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
        tilesCollectedBySpace = tilesCollected.associateByTo(
            HashMap((tilesCollected.size / 0.75F + 1.0F).toInt())
        ) { it.spaceKey() }
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

        /* Always perform partial eviction */
        partialEviction(visibleTiles, layerId)

        /* Only perform aggressive eviction when tile collector is idle */
        if (aggressiveAttempt && tileCollector.isIdle) {
            aggressiveEviction(currentLevel, layerId)
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
        val addedSet = hashSetOf<SpaceKey>()

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

            if (tile.zoom == currentLevel && !visibleTiles.contains(tile)) {
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
        layerId: String
    ) {
        val iterator = tilesCollected.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()

            /* Remove tiles at different level or from other layers */
            if (tile.zoom != currentLevel || tile.layerId != layerId) {
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
