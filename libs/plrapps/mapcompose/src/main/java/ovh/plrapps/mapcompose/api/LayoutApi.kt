@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.state.MapModel
import ovh.plrapps.mapcompose.utils.Point
import ovh.plrapps.mapcompose.utils.ScaledCamera
import ovh.plrapps.mapcompose.utils.throttle
import ovh.plrapps.mapcompose.utils.withRetry
import kotlin.math.min

/**
 * The scale of the map. By convention, the scale at full dimension is 1.0.
 */
var MapModel.scale: Double
    get() = zoomPanState.scale
    set(value) {
        zoomPanState.setScale(value)
    }

/**
 * Get the current [camera] - the position of the canter of the visible viewport.
 * This is a low-level concept (returned value is in absolute coordinates).
 */
val MapModel.camera: Camera
    get() = Camera(zoomPanState.cameraX, zoomPanState.cameraY)


/**
 * Set the [camera] - the position of the center of the visible viewport. This is a
 * It's invoked immediately as absolute coordinates do not rely on layout size.
 */
fun MapModel.setCamera(x: Double, y: Double) {
    zoomPanState.setCamera(x, y)
}


/**
 * The default minimum scale is [Double.MIN_VALUE].
 * When changed, and if the current scale is smaller than the new [minScale], the current scale is
 * changed to be equal to [minScale].
 */
var MapModel.minScale: Double
    get() = zoomPanState.minScale
    set(value) {
        zoomPanState.minScale = value
    }

/**
 * The default maximum scale is 4.0.
 * When changed, and if the current scale is greater than the new [maxScale], the current scale is
 * changed to be equal to [maxScale].
 */
var MapModel.maxScale: Double
    get() = zoomPanState.maxScale
    set(value) {
        zoomPanState.maxScale = value
    }

/**
 * Get the layout dimensions in pixels.
 * Note that layout dimension may change during the lifetime of the application. The returned value
 * is a read-only snapshot.
 */
suspend fun MapModel.getLayoutSize(): IntSize {
    awaitLayout()
    return layoutSize
}

/**
 * Get the layout dimensions in pixels, as a [Flow].
 * This api is useful to observe layout changes.
 */
suspend fun MapModel.getLayoutSizeFlow(): Flow<IntSize> {
    awaitLayout()
    return layoutFlow
}

/**
 * Scrolls to a position, animating the scroll and the scale. Defaults to centering on the provided
 * scroll destination.
 * Requires [androidx.compose.runtime.MonotonicFrameClock].
 *
 * @param x The absolute X position on the map
 * @param y The absolute Y position on the map
 * @param destScale The destination scale. The default value is the current scale.
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
suspend fun MapModel.scrollTo(
    x: Double,
    y: Double,
    destScale: Double = scale,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
) {
    awaitLayout()
    val effectiveDstScale = zoomPanState.constrainScale(destScale)

    val paddingOffset = visibleAreaPadding.getOffsetForScroll()
    val destScrollX = x - paddingOffset.x / effectiveDstScale
    val destScrollY = y - paddingOffset.y / effectiveDstScale

    withRetry(maxAnimationsRetries, animationsRetriesInterval) {
        zoomPanState.smoothScrollScale(
            destScrollX,
            destScrollY,
            effectiveDstScale,
            animationSpec
        )
    }
}

/**
 * Scrolls to an area. The target position will be centered on the area, scaled in as much as
 * possible while still keeping the area plus the provided padding completely in view.
 *
 * @param area The [BoundingBox] of the target area to scroll to.
 * @param padding Padding around the area defined as a fraction of the viewport.
 */
suspend fun MapModel.snapScrollTo(
    area: BoundingBox,
    padding: Offset = Offset.Zero
) {
    awaitLayout()
    val state = calculateScrollTo(area, padding)
    zoomPanState.setScale(state.scale)

    val paddingOffset = visibleAreaPadding.getOffsetForScroll()
    zoomPanState.setCamera(
        state.x - paddingOffset.x / zoomPanState.scale,
        state.y - paddingOffset.y / zoomPanState.scale
    )
}

/**
 * Scrolls to an area, animating the scroll and the scale. The target position will be centered
 * on the area, scaled in as much as possible while still keeping the area plus the provided
 * padding completely in view.
 * Requires [androidx.compose.runtime.MonotonicFrameClock].
 *
 * @param area The [BoundingBox] of the target area to scroll to.
 * @param padding Padding around the area defined as a fraction of the viewport.
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
suspend fun MapModel.scrollTo(
    area: BoundingBox,
    padding: Offset = Offset.Zero,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
) {
    awaitLayout()
    val state = calculateScrollTo(area, padding)
    scrollTo(state.x, state.y, state.scale, animationSpec)
}

/**
 * Calculates the target scroll position and scale that will be centered on the given [area] and
 * scaled in as much as possible while keeping the [area] plus [padding] in view.
 *
 * @return The scroll position and scale, as a [ScaledCamera].
 */
private fun MapModel.calculateScrollTo(
    area: BoundingBox,
    padding: Offset
): ScaledCamera {
    val centerX = (area.xLeft + area.xRight) / 2
    val centerY = (area.yTop + area.yBottom) / 2

    val areaWidth = area.xRight - area.xLeft
    val availableViewportWidth = (layoutSize.width - visibleAreaPadding.x) * (1 - padding.x)
    val horizontalScale = availableViewportWidth / areaWidth

    val areaHeight = area.yBottom - area.yTop
    val availableViewportHeight = (layoutSize.height - visibleAreaPadding.y) * (1 - padding.y)
    val verticalScale = availableViewportHeight / areaHeight

    val targetScale = min(horizontalScale, verticalScale)
    val effectiveTargetScale = zoomPanState.constrainScale(targetScale)

    return ScaledCamera(centerX, centerY, effectiveTargetScale)
}

/**
 * The [cameraX] is the x coordinate of the center of the current viewport.
 * It changes with the scroll and the scale.
 * This is a low-level concept, and is only useful when defining custom views.
 * The value is a absolute coordinate.
 */
val MapModel.cameraX: Double
    get() = zoomPanState.cameraX

/**
 * The [cameraY] is the y coordinate of the center of the current viewport.
 * It changes with the scroll and the scale.
 * This is a low-level concept, and is only useful when defining custom views.
 * The value is a absolute coordinate.
 */
val MapModel.cameraY: Double
    get() = zoomPanState.cameraY

/**
 * Get the flow of centroid points. A centroid point contains the absolute coordinates of the
 * center of the map.
 * Useful for asynchronous processing using flow operators. Like every snapshot flow, it should be
 * collected from the main thread.
 *
 * Example:
 * ```
 * mapModel.centroidSnapshotFlow().map { point ->
 *   withContext(Dispatchers.Default) {
 *     // some heavy computing
 *   }
 * }.launchIn(scope)  // scope is using Dispatchers.Main
 * ```
 */
fun MapModel.centroidSnapshotFlow(): Flow<Point> = snapshotFlow {
    Point(cameraX, cameraY)
}

/**
 * Returns the level, an entire value belonging to [0 ; levelCount - 1], where `levelCount` is the
 * count of levels passed at the [MapModel] constructor.
 */
fun MapModel.getLevelAtScale(scale: Double): Int =
    visibleTilesResolver.getLevel(scale, visibleTilesResolver.magnifyingFactor)

/**
 * Stops all currently running animations. If other animations are scheduled to run (inside running
 * coroutines), you might have to cancel those coroutines as well.
 */
suspend fun MapModel.stopAnimations() {
    zoomPanState.stopAnimations()
}

/**
 * Returns the visible area expressed in absolute coordinates.
 * The obtained [BoundingBox] represents the same area as the one
 * obtained with the [visibleArea] API.
 */
suspend fun MapModel.visibleBoundingBox(): BoundingBox {
    awaitLayout()
    val width = layoutSize.width / scale
    val height = layoutSize.height / scale
    val left = cameraX - width / 2
    val top = cameraY - height / 2
    return BoundingBox(
        xLeft = left,
        yTop = top,
        xRight = left + width,
        yBottom = top + height,
    )
}


/**
 * Returns the visible area expressed in absolute coordinates.
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
suspend fun MapModel.visibleArea(padding: IntOffset = IntOffset.Zero): VisibleArea {
    awaitLayout()
    val layoutSize = this.layoutSize
    val width = (layoutSize.width + padding.x * 2) / scale
    val height = (layoutSize.height + padding.y * 2) / scale
    val left = cameraX - width / 2
    val top = cameraY - height / 2

    return VisibleArea(left, top, left + width, top + height)
}

/**
 * Returns the visible area expressed in absolute coordinates.
 */
suspend fun MapModel.visibleAreaFlow(
    padding: IntOffset = IntOffset.Zero,
    throttleMillis: Long = 500
): Flow<VisibleArea> = viewportSnapshotFlow().throttle(throttleMillis).map {
    visibleArea(padding)
}

/**
 * Returns a flow which emits whenever the viewport changes.
 * It's the flow-based replacement for the old state change listener.
 */
fun MapModel.viewportSnapshotFlow(): Flow<ScaledCamera> = snapshotFlow {
    ScaledCamera(cameraX, cameraY, scale)
}

/**
 * The [MapModel] is considered idle when its [cameraX] and [cameraY] haven't changed for at
 * least [thresholdMillis] which is 400ms by default.
 */
fun MapModel.idleStateFlow(thresholdMillis: Long = 400): StateFlow<Boolean> {
    val stateFlow = MutableStateFlow(false)
    viewModelScope.launch {
        centroidSnapshotFlow().map {
            stateFlow.value = false
        }.collectLatest {
            delay(thresholdMillis)
            stateFlow.value = true
        }
    }

    return stateFlow.asStateFlow()
}
