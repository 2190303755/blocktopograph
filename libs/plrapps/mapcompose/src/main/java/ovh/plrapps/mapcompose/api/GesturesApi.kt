@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.ui.state.MapModel

/**
 * Enable scrolling by user gestures. This is enabled by default.
 */
fun MapModel.enableScrolling() {
    zoomPanState.isScrollingEnabled = true
}

/**
 * Enable zooming by user gestures. This is enabled by default.
 */
fun MapModel.enableZooming() {
    zoomPanState.isZoomingEnabled = true
}

/**
 * Discard scrolling gestures. The map can still be programmatically scrolled using APIs such as
 * [scrollTo] or [snapScrollTo].
 */
fun MapModel.disableScrolling() {
    zoomPanState.isScrollingEnabled = false
}

/**
 * Discard zooming gestures. The map can still be programmatically zoomed using [scale].
 */
fun MapModel.disableZooming() {
    zoomPanState.isZoomingEnabled = false
}

/**
 * Disable gesture detection. The map view can still be transformed programmatically.
 */
fun MapModel.disableGestures() {
    disableScrolling()
    disableZooming()
}

/**
 * Enables fling scale animation after a pinch to zoom gesture. Enabled by default.
 */
fun MapModel.enableFlingZoom() {
    zoomPanState.isFlingZoomEnabled = true
}

/**
 * Disables fling scale animation after a pinch to zoom gesture.
 */
fun MapModel.disableFlingZoom() {
    zoomPanState.isFlingZoomEnabled = false
}
