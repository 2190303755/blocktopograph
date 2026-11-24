package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ovh.plrapps.mapcompose.core.GestureConfiguration
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.core.throttle
import kotlin.time.Duration.Companion.milliseconds

/**
 * The state of the map. All public APIs are extensions functions or extension properties of this
 * class.
 *
 * @param levelCount The number of levels in the pyramid.
 * @param tileSize The size in pixels of tiles, which are expected to be squared. Defaults to 256.
 * @param workerCount The thread count used to fetch tiles. Defaults to the number of cores minus
 * one, which works well for tiles in the file system or in a local database. However, that number
 * should be increased to 16 or more for remote tiles (HTTP requests).
 * @param scale The initial scale. Defaults to 1.0.
 * @param minScale The minimum scale. Defaults to [Double.MIN_VALUE].
 * @param maxScale The maximum scale. Defaults to 4.0.
 * @param magnifyingFactor The magnifying factor used when resolving the level for a given scale.
 * Defaults to 0, which picks the level immediately higher in index to avoid sub-sampling.
 * @param preloadingPadding The extra pixels preloaded around the visible area. Defaults to 0.
 * @param gestureConfiguration The configuration of gesture animations. Defaults to
 * [GestureConfiguration].
 */
class MapModel(
    levelCount: Int,
    tileSize: Int = 256,
    workerCount: Int = Runtime.getRuntime().availableProcessors() - 1,
    scale: Double = 1.0,
    minScale: Double = Double.MIN_VALUE,
    maxScale: Double = 4.0,
    magnifyingFactor: Int = 0,
    preloadingPadding: Int = 0,
    gestureConfiguration: GestureConfiguration = GestureConfiguration(),
) : ViewModel() {
    internal val zoomPanState = ZoomPanState(
        stateChangeListener = { renderVisibleTilesThrottled() },
        minScale = minScale,
        maxScale = maxScale,
        scale = scale,
        gestureConfiguration = gestureConfiguration,
    )
    internal val visibleTilesResolver = VisibleTilesResolver(
        levelCount = levelCount,
        tileSize = tileSize,
        magnifyingFactor = magnifyingFactor
    )
    internal val tileCanvasState = TileCanvasState(
        viewModelScope,
        visibleTilesResolver,
        workerCount
    )

    private val throttledTask = viewModelScope.throttle(wait = 18.milliseconds) {
        updateViewport()
    }

    internal var preloadingPadding: Int = preloadingPadding.coerceAtLeast(0)
    internal val tileSize get() = visibleTilesResolver.tileSize

    val layoutSize: IntSize get() = zoomPanState.layoutSize
    val visibleAreaPadding: IntOffset get() = zoomPanState.visibleAreaPadding

    /**
     * Layout sizes, without continuations. Because the visible-area offset is a separate state,
     * offset-only changes do not emit here; a zero size is filtered by [awaitLayout].
     */
    val layoutFlow: Flow<IntSize> = snapshotFlow { layoutSize }

    suspend fun awaitLayout(): IntSize = layoutFlow.first { it != IntSize.Zero }

    internal fun renderVisibleTilesThrottled() {
        throttledTask.trySend(Unit)
    }

    private fun updateViewport() {
        val padding = preloadingPadding * 2
        val zoomPanState = this.zoomPanState
        val scale = zoomPanState.scale
        val layoutSize = zoomPanState.layoutSize
        val width = padding + layoutSize.width
        val height = padding + layoutSize.height
        val left = (zoomPanState.cameraX * scale).toInt() - width / 2
        val top = (zoomPanState.cameraY * scale).toInt() - height / 2
        tileCanvasState.setViewport(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
            scale = scale
        )
    }

    override fun onCleared() {
        /* Order matters: cancelling first prevents a late reply from hanging on a RENDEZVOUS
         * channel with no consumer, then reject new tasks, then interrupt the workers. */
        viewModelScope.cancel()
        tileCanvasState.close()
    }
}
