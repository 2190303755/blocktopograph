package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ovh.plrapps.mapcompose.core.GestureConfiguration
import ovh.plrapps.mapcompose.core.Viewport
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.core.throttle
import ovh.plrapps.mapcompose.ui.gestures.model.HitType
import ovh.plrapps.mapcompose.ui.state.markers.MarkerRenderState
import ovh.plrapps.mapcompose.ui.state.markers.MarkerState
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
 * @param initialValuesBuilder A builder for [InitialValues] which are applied during [MapState]
 * initialization. Note that the provided lambda should not start any coroutines.
 */
class MapState(
    levelCount: Int,
    tileSize: Int = 256,
    workerCount: Int = Runtime.getRuntime().availableProcessors() - 1,
    initialValuesBuilder: InitialValues.() -> Unit = {}
) : ZoomPanStateListener {
    private val initialValues = InitialValues().apply(initialValuesBuilder)
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    internal val zoomPanState = ZoomPanState(
        stateChangeListener = this,
        minScale = initialValues.minScale,
        maxScale = initialValues.maxScale,
        scale = initialValues.scale,
        gestureConfiguration = initialValues.gestureConfiguration
    )
    internal val markerRenderState = MarkerRenderState()
    internal val markerState = MarkerState(scope, markerRenderState)
    internal val pathState = PathState()
    internal val visibleTilesResolver =
        VisibleTilesResolver(
            levelCount = levelCount,
            tileSize = tileSize,
            magnifyingFactor = initialValues.magnifyingFactor
        ) {
            zoomPanState.scale
        }
    internal val tileCanvasState = TileCanvasState(
        scope,
        visibleTilesResolver,
        workerCount
    )

    private val throttledTask = scope.throttle(wait = 18.milliseconds) {
        renderVisibleTiles()
    }
    private val viewport = Viewport()
    internal var preloadingPadding: Int = initialValues.preloadingPadding
    internal val tileSize by mutableIntStateOf(tileSize)
    internal var stateChangeListener: (MapState.() -> Unit)? = null
    internal var touchDownCb: (() -> Unit)? = null
    internal var tapCb: LayoutTapCb? = null
    internal var longPressCb: LayoutTapCb? = null
    internal var isFilteringBitmap: () -> Boolean by mutableStateOf(
        { initialValues.isFilteringBitmap(this) }
    )

    /**
     * Cancels all internal tasks.
     * After this call, this [MapState] is unusable.
     */
    @Suppress("unused")
    fun shutdown() {
        scope.cancel()
        tileCanvasState.shutdown()
        pathState.removeAllPaths()
        markerState.removeAllMarkers()
    }

    override fun onStateChanged() {
        renderVisibleTilesThrottled()
        stateChangeListener?.invoke(this)
    }

    override fun onTouchDown() {
        touchDownCb?.invoke()
    }

    override fun onPress() {
        markerRenderState.removeAllAutoDismissCallouts()
    }

    override fun onLongPress(x: Double, y: Double) {
        longPressCb?.invoke(x, y)
    }

    override fun onTap(x: Double, y: Double) {
        tapCb?.invoke(x, y)
    }

    override fun detectsTap(): Boolean = tapCb != null

    override fun detectsLongPress(): Boolean = longPressCb != null

    override fun interceptsTap(x: Double, y: Double, xPx: Int, yPx: Int): Boolean {
        return markerState.onHit(xPx, yPx, hitType = HitType.Click)
                || pathState.onHit(x, y, zoomPanState.scale, hitType = HitType.Click)
    }

    override fun interceptsLongPress(x: Double, y: Double, xPx: Int, yPx: Int): Boolean {
        return markerState.onHit(xPx, yPx, hitType = HitType.LongPress)
                || pathState.onHit(x, y, zoomPanState.scale, hitType = HitType.LongPress)
    }

    internal fun renderVisibleTilesThrottled() {
        throttledTask.trySend(Unit)
    }

    private suspend fun renderVisibleTiles() {
        val viewport = updateViewport()
        tileCanvasState.setViewport(viewport)
    }

    private fun updateViewport(): Viewport {
        val padding = preloadingPadding * 2
        val zoomPanState = this.zoomPanState
        val layoutSize = zoomPanState.layoutSize
        val width = padding + layoutSize.width
        val height = padding + layoutSize.height
        return viewport.apply {
            left = (zoomPanState.cameraX * zoomPanState.scale).toInt() - width / 2
            top = (zoomPanState.cameraY * zoomPanState.scale).toInt() - height / 2
            right = left + width
            bottom = top + height
        }
    }
}

/**
 * Builder for initial values.
 * Changes made after the `MapState` instance creation take precedence over initial values.
 * In the following example, the init scale will be 4.0 since the max scale is later set to 4.0.
 *
 * ```
 * MapState(4, 4096, 4096,
 *   initialValues = InitialValues().scale(8.0)
 * ).apply {
 *   addLayer(tileStreamProvider)
 *   maxScale = 4.0
 * }
 * ```
 */
@Suppress("unused")
class InitialValues internal constructor() {
    internal var scale: Double = 1.0
    internal var minScale: Double = Double.MIN_VALUE
    internal var maxScale: Double = 4.0
    internal var magnifyingFactor = 0
    internal var preloadingPadding: Int = 0
    internal var isFilteringBitmap: (MapState) -> Boolean = { true }
    internal var gestureConfiguration: GestureConfiguration = GestureConfiguration()

    /**
     * Set the initial scale. Defaults to 1.0.
     */
    fun scale(scale: Double) = apply {
        this.scale = scale
    }

    /**
     * Set the minimum allowed scale. Defaults to [Double.MIN_VALUE].
     */
    fun minScale(minScale: Double) = apply {
        this.minScale = minScale
    }

    /**
     * Set the maximum allowed scale. Defaults to 4.0.
     */
    fun maxScale(maxScale: Double) = apply {
        this.maxScale = maxScale
    }

    /**
     * Alters the level at which tiles are picked for a given scale. By default, the level
     * immediately higher (in index) is picked, to avoid sub-sampling. This corresponds to a
     * [magnifyingFactor] of 0. The value 1 will result in picking the current level at a given
     * scale, which will be at a relative scale between 1.0 and 2.0
     */
    fun magnifyingFactor(magnifyingFactor: Int) = apply {
        this.magnifyingFactor = magnifyingFactor.coerceAtLeast(0)
    }

    /**
     * By default, only visible tiles are loaded. By adding a preloadingPadding additional tiles
     * will be loaded, which can be used to produce a seamless tile loading effect.
     *
     * @param padding in pixels
     */
    fun preloadingPadding(padding: Int) = apply {
        this.preloadingPadding = padding.coerceAtLeast(0)
    }

    /**
     * Controls whether Bitmap filtering is enabled when drawing tiles. This is enabled by default.
     * Disabling it is useful to achieve nearest-neighbor scaling, for cases when the art style of
     * the displayed image benefits from it.
     * @see [android.graphics.Paint.setFilterBitmap]
     */
    fun bitmapFilteringEnabled(enabled: Boolean) = apply {
        bitmapFilteringEnabled { enabled }
    }

    /**
     * A version of [bitmapFilteringEnabled] which allows for dynamic control of bitmap filtering
     * depending on the current [MapState].
     */
    fun bitmapFilteringEnabled(predicate: (state: MapState) -> Boolean) = apply {
        isFilteringBitmap = predicate
    }

    /**
     * Customize gestures.
     */
    fun configureGestures(gestureConfigurationBlock: GestureConfiguration.() -> Unit) {
        this.gestureConfiguration.gestureConfigurationBlock()
    }
}

internal typealias LayoutTapCb = (x: Double, y: Double) -> Unit