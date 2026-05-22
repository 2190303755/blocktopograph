@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.ui.platform.ViewConfiguration
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * Enable scrolling by user gestures. This is enabled by default.
 */
fun MapState.enableScrolling() {
    zoomPanState.isScrollingEnabled = true
}

/**
 * Enable zooming by user gestures. This is enabled by default.
 */
fun MapState.enableZooming() {
    zoomPanState.isZoomingEnabled = true
}

/**
 * Discard scrolling gestures. The map can still be programmatically scrolled using APIs such as
 * [scrollTo] or [snapScrollTo].
 */
fun MapState.disableScrolling() {
    zoomPanState.isScrollingEnabled = false
}

/**
 * Discard zooming gestures. The map can still be programmatically zoomed using [scale].
 */
fun MapState.disableZooming() {
    zoomPanState.isZoomingEnabled = false
}

/**
 * Disable gesture detection. The map view can still be transformed programmatically.
 */
fun MapState.disableGestures() {
    with(zoomPanState) {
        isScrollingEnabled = false
        isZoomingEnabled = false
    }
}

/**
 * Enables fling scale animation after a pinch to zoom gesture. Enabled by default.
 */
fun MapState.enableFlingZoom() {
    zoomPanState.isFlingZoomEnabled = true
}

/**
 * Disables fling scale animation after a pinch to zoom gesture.
 */
fun MapState.disableFlingZoom() {
    zoomPanState.isFlingZoomEnabled = false
}

/**
 * Registers a tap callback for tap gestures. The callback is invoked with the relative coordinates
 * of the tapped point on the map.
 * Note: the tap gesture is detected only after the [ViewConfiguration.doubleTapMinTimeMillis] has
 * passed, because the layout's gesture detector also detects double-tap gestures.
 */
fun MapState.onTap(tapCb: (x: Double, y: Double) -> Unit) {
    this.tapCb = tapCb
}

/**
 * Registers a callback for long press gestures. The callback is invoked with the relative coordinates
 * of the pressed point on the map.
 */
fun MapState.onLongPress(longPressCb: (x: Double, y: Double) -> Unit) {
    this.longPressCb = longPressCb
}

/**
 * Registers a callback for touch down event.
 */
fun MapState.onTouchDown(cb: () -> Unit) {
    touchDownCb = cb
}