@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.VisibleAreaPadding
import ovh.plrapps.mapcompose.ui.state.ZoomPanState
import ovh.plrapps.mapcompose.utils.Point
import ovh.plrapps.mapcompose.utils.dpToPx
import ovh.plrapps.mapcompose.utils.throttle
import ovh.plrapps.mapcompose.utils.withRetry
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The scale of the map. By convention, the scale at full dimension is 1.0.
 */
var MapState.scale: Double
    get() = zoomPanState.scale
    set(value) {
        zoomPanState.setScale(value)
    }

/**
 * Get the current [camera] - the position of the canter of the visible viewport.
 * This is a low-level concept (returned value is in scaled pixels).
 */
val MapState.camera: Camera
    get() = Camera(zoomPanState.cameraX, zoomPanState.cameraY)


/**
 * Set the [camera] - the position of the center of the visible viewport. This is a
 * suspending call because it's required to wait the first composition. Otherwise, it's invoked
 * immediately.
 */
suspend fun MapState.setCamera(x: Double, y: Double) {
    with(zoomPanState) {
        awaitLayout()

        setCamera(x, y)
    }
}

fun MapState.referentialSnapshotFlow(): Flow<ReferentialSnapshot> = snapshotFlow {
    ReferentialSnapshot(zoomPanState.scale, camera)
}

data class ReferentialSnapshot(val scale: Double, val camera: Camera)

/**
 * Get notified whenever the state ([scale] and/or [camera]) changes.
 *
 * @param cb An extension function with [MapState] as receiver type
 */
fun MapState.setStateChangeListener(cb: MapState.() -> Unit) {
    stateChangeListener = cb
}

/**
 * Removes the state change listener.
 */
fun MapState.removeStateChangeListener() {
    stateChangeListener = null
}

/**
 * Sets the padding of the visible area of the map viewport in [Dp], for the purpose of camera moves.
 * For example, if you have some UI obscuring the map on the left, you can set the appropriate
 * left padding. Then, when you use the scrollTo methods, the map will take that into account, by
 * centering on the visible portion of the viewport.
 */
fun MapState.setVisibleAreaPadding(
    left: Dp = 0.dp,
    right: Dp = 0.dp,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp
) {
    setVisibleAreaPadding(
        left = dpToPx(left.value).roundToInt(),
        right = dpToPx(right.value).roundToInt(),
        top = dpToPx(top.value).roundToInt(),
        bottom = dpToPx(bottom.value).roundToInt()
    )
}

/**
 * Variant of [MapState.setVisibleAreaPadding] using ratios. This is a suspending call because it
 * awaits for the first layout.
 *
 * @param leftRatio The left padding will be equal to this ratio multiplied by the layout width.
 * @param rightRatio The right padding will be equal to this ratio multiplied by the layout width.
 * @param topRatio The top padding will be equal to this ratio multiplied by the layout height.
 * @param bottomRatio The bottom padding will be equal to this ratio multiplied by the layout height.
 */
suspend fun MapState.setVisibleAreaPadding(
    leftRatio: Float = 0f,
    rightRatio: Float = 0f,
    topRatio: Float = 0f,
    bottomRatio: Float = 0f
) {
    with(zoomPanState) {
        awaitLayout()
        val layoutSize = zoomPanState.layoutSize
        setVisibleAreaPadding(
            left = (leftRatio * layoutSize.width).roundToInt(),
            right = (rightRatio * layoutSize.width).roundToInt(),
            top = (topRatio * layoutSize.height).roundToInt(),
            bottom = (bottomRatio * layoutSize.height).roundToInt()
        )
    }
}

/**
 * Variant of [MapState.setVisibleAreaPadding] using pixels.
 */
fun MapState.setVisibleAreaPadding(left: Int = 0, right: Int = 0, top: Int = 0, bottom: Int = 0) {
    zoomPanState.visibleAreaPadding = VisibleAreaPadding(left, top, right, bottom)
}

/**
 * The default minimum scale is [Double.MIN_VALUE].
 * When changed, and if the current scale is smaller than the new [minScale], the current scale is
 * changed to be equal to [minScale].
 */
var MapState.minScale: Double
    get() = zoomPanState.minScale
    set(value) {
        zoomPanState.minScale = value
    }

/**
 * The default maximum scale is 2.0.
 * When changed, and if the current scale is greater than the new [maxScale], the current scale is
 * changed to be equal to [maxScale].
 */
var MapState.maxScale: Double
    get() = zoomPanState.maxScale
    set(value) {
        zoomPanState.maxScale = value
    }

/**
 * Get the layout dimensions in pixels.
 * Note that layout dimension may change during the lifetime of the application. The returned value
 * is a read-only snapshot.
 */
suspend fun MapState.getLayoutSize(): IntSize {
    return with(zoomPanState) {
        awaitLayout()
        layoutSize
    }
}

/**
 * Get the layout dimensions in pixels, as a [Flow].
 * This api is useful to observe layout changes.
 */
suspend fun MapState.getLayoutSizeFlow(): Flow<IntSize> {
    return with(zoomPanState) {
        awaitLayout()
        snapshotFlow {
            layoutSize
        }
    }
}

/**
 * Scrolls to a position, animating the scroll and the scale. Defaults to centering on the provided
 * scroll destination.
 *
 * @param x The absolute X position on the map
 * @param y The absolute Y position on the map
 * @param destScale The destination scale. The default value is the current scale.
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
suspend fun MapState.scrollTo(
    x: Double,
    y: Double,
    destScale: Double = scale,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
) {
    with(zoomPanState) {
        awaitLayout()
        val effectiveDstScale = constrainScale(destScale)

        val paddingOffset = visibleAreaPadding.getOffsetForScroll()
        val destScrollX = x - paddingOffset.x / effectiveDstScale
        val destScrollY = y - paddingOffset.y / effectiveDstScale

        withRetry(maxAnimationsRetries, animationsRetriesInterval) {
            smoothScrollScale(
                destScrollX,
                destScrollY,
                effectiveDstScale,
                animationSpec
            )
        }
    }
}

/**
 * Scrolls to an area. The target position will be centered on the area, scaled in as much as
 * possible while still keeping the area plus the provided padding completely in view.
 *
 * @param area The [BoundingBox] of the target area to scroll to.
 * @param padding Padding around the area defined as a fraction of the viewport.
 */
suspend fun MapState.snapScrollTo(
    area: BoundingBox,
    padding: Offset = Offset(0f, 0f)
) {
    with(zoomPanState) {
        awaitLayout()
        val (center, scale) = calculateScrollTo(area, padding)
        setScale(scale)

        val paddingOffset = visibleAreaPadding.getOffsetForScroll()
        val destScrollX = center.x - paddingOffset.x / this.scale
        val destScrollY = center.y - paddingOffset.y / this.scale

        setCamera(destScrollX, destScrollY)
    }
}

/**
 * Scrolls to an area, animating the scroll and the scale. The target position will be centered
 * on the area, scaled in as much as possible while still keeping the area plus the provided
 * padding completely in view.
 *
 * @param area The [BoundingBox] of the target area to scroll to.
 * @param padding Padding around the area defined as a fraction of the viewport.
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
suspend fun MapState.scrollTo(
    area: BoundingBox,
    padding: Offset = Offset(0f, 0f),
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
) {
    with(zoomPanState) {
        awaitLayout()
        val (center, scale) = calculateScrollTo(area, padding)
        scrollTo(center.x, center.y, scale, animationSpec)
    }
}

/**
 * Calculates the target scroll position and scale that will be centered on the given [area] and
 * scaled in as much as possible while keeping the [area] plus [padding] in view.
 *
 * @return The scroll position and scale, as a [Pair].
 */
private fun ZoomPanState.calculateScrollTo(
    area: BoundingBox,
    padding: Offset
): Pair<Point, Double> {
    val centerX = (area.xLeft + area.xRight) / 2
    val centerY = (area.yTop + area.yBottom) / 2

    val areaWidth = area.xRight - area.xLeft
    val availableViewportWidth = (layoutSize.width - visibleAreaPadding.left - visibleAreaPadding.right) * (1 - padding.x)
    val horizontalScale = availableViewportWidth / areaWidth

    val areaHeight = area.yBottom - area.yTop
    val availableViewportHeight = (layoutSize.height - visibleAreaPadding.top - visibleAreaPadding.bottom) * (1 - padding.y)
    val verticalScale = availableViewportHeight / areaHeight

    val targetScale = min(horizontalScale, verticalScale)
    val effectiveTargetScale = constrainScale(targetScale)

    return Point(centerX, centerY) to effectiveTargetScale
}

/**
 * The [cameraX] is the x coordinate of the center of the current viewport.
 * It changes with the scroll and the scale.
 * This is a low-level concept, and is only useful when defining custom views.
 * The value is a absolute coordinate.
 */
val MapState.cameraX: Double
    get() = zoomPanState.cameraX

/**
 * The [cameraY] is the y coordinate of the center of the current viewport.
 * It changes with the scroll and the scale.
 * This is a low-level concept, and is only useful when defining custom views.
 * The value is a absolute coordinate.
 */
val MapState.cameraY: Double
    get() = zoomPanState.cameraY

/**
 * Get the flow of centroid points. A centroid point contains the absolute coordinates of the
 * center of the map.
 * Useful for asynchronous processing using flow operators. Like every snapshot flow, it should be
 * collected from the main thread.
 *
 * Example:
 * ```
 * mapState.centroidSnapshotFlow().map { point ->
 *   withContext(Dispatchers.Default) {
 *     // some heavy computing
 *   }
 * }.launchIn(scope)  // scope is using Dispatchers.Main
 * ```
 */
fun MapState.centroidSnapshotFlow(): Flow<Point> {
    return snapshotFlow {
        Point(zoomPanState.cameraX, zoomPanState.cameraY)
    }
}

/**
 * Returns the level, an entire value belonging to [0 ; levelCount - 1], where `levelCount` is the
 * count of levels passed at [MapState] constructor.
 */
fun MapState.getLevelAtScale(scale: Double): Int {
    return visibleTilesResolver.getLevel(scale)
}

/**
 * Stops all currently running animations. If other animations are scheduled to run (inside running
 * coroutines), you might have to cancel those coroutines as well.
 */
suspend fun MapState.stopAnimations() {
    zoomPanState.stopAnimations()
}

/**
 * Returns the visible area expressed in normalized coordinates.
 * The obtained [BoundingBox] represents the same area as the one
 * obtained with the [visibleArea] API.
 */
suspend fun MapState.visibleBoundingBox(): BoundingBox {
    return with(zoomPanState) {
        awaitLayout()

        val width = layoutSize.width / scale
        val height = layoutSize.height / scale
        val left = cameraX - width / 2
        val top = cameraY - height / 2
        BoundingBox(
            xLeft = left,
            yTop = top,
            xRight = left + width,
            yBottom = top + height
        )
    }
}

data class BoundingBox(val xLeft: Double, val yTop: Double, val xRight: Double, val yBottom: Double)

/**
 * Returns the visible area expressed in normalized coordinates.
 *
 * @return The [VisibleArea], as follows:
 * ```
 *    p1         p2
 *      ---------
 *      |       |
 *      |       |
 *      ---------
 *    p4         p3
 * ```
 */
suspend fun MapState.visibleArea(padding: IntOffset = IntOffset.Zero): VisibleArea {
    return with(zoomPanState) {
        awaitLayout()
        val width = (layoutSize.width + padding.x * 2) / scale
        val height = (layoutSize.height + padding.y * 2) / scale
        val xLeft = cameraX - width / 2
        val yTop = cameraY - height / 2
        val xRight = cameraX + width
        val yBottom = cameraY + height

        visibleAreaMutex.withLock {
            val area = visibleArea
            if (area == null) {
                visibleArea = VisibleArea(
                    xLeft, yTop, xRight, yTop, xRight, yBottom, xLeft,
                    yBottom
                )
            } else {
                area._p1x = xLeft
                area._p1y = yTop
                area._p2x = xRight
                area._p2y = yTop
                area._p3x = xRight
                area._p3y = yBottom
                area._p4x = xLeft
                area._p4y = yBottom
            }
            visibleArea as VisibleArea
        }
    }
}

/**
 * Returns the visible area expressed in normalized coordinates.
 */
suspend fun MapState.visibleAreaFlow(
    padding: IntOffset = IntOffset.Zero,
    throttleMillis: Long = 500
): Flow<VisibleArea> {
    return snapshotFlow {
        cameraX.hashCode() + cameraY.hashCode() + scale.hashCode()
    }.throttle(throttleMillis).map {
        visibleArea(padding)
    }
}

/**
 * ```
 *    p1         p2
 *      ---------
 *      |       |
 *      |       |
 *      ---------
 *    p4         p3
 * ```
 */
data class VisibleArea(
    internal var _p1x: Double,
    internal var _p1y: Double,
    internal var _p2x: Double,
    internal var _p2y: Double,
    internal var _p3x: Double,
    internal var _p3y: Double,
    internal var _p4x: Double,
    internal var _p4y: Double,
) {
    val p1x: Double
        get() = _p1x
    val p1y: Double
        get() = _p1y
    val p2x: Double
        get() = _p2x
    val p2y: Double
        get() = _p2y
    val p3x: Double
        get() = _p3x
    val p3y: Double
        get() = _p3y
    val p4x: Double
        get() = _p4x
    val p4y: Double
        get() = _p4y
}

/* Internally, we're working on a single VisibleArea instance, and we must ensure mutual exclusion
 * when creating the instance. */
internal val visibleAreaMutex = Mutex()
internal var visibleArea: VisibleArea? = null

/**
 * Returns a flow which emits [MapState] whenever the viewport changes.
 * It's a flow equivalent of [setStateChangeListener].
 */
fun MapState.viewportChangeFlow(): Flow<MapState> {
    return snapshotFlow {
        cameraX.hashCode() + cameraY.hashCode() + scale.hashCode()
    }.map { this }
}

/**
 * The [MapState] is considered idle when its [cameraX] and [cameraY] haven't changed for at
 * least [thresholdMillis] which is 400ms by default.
 */
fun MapState.idleStateFlow(thresholdMillis: Long = 400): StateFlow<Boolean> {
    val stateFlow = MutableStateFlow(false)

    scope.launch {
        snapshotFlow {
            "$cameraX,$cameraY"
        }.map {
            stateFlow.value = false
        }.collectLatest {
            delay(thresholdMillis)
            stateFlow.value = true
        }
    }

    return stateFlow.asStateFlow()
}


