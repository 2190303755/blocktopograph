@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.LayerFactory
import ovh.plrapps.mapcompose.core.TileBitmapProvider
import ovh.plrapps.mapcompose.ui.state.MapState


fun MapState.setLayer(name: String, layer: TileBitmapProvider) {
    this.setLayer(LayerFactory(name, layer))
}

fun MapState.clearLayer() {
    this.setLayer(null)
}

val MapState.layerId: String?
    get() = tileCanvasState.layer?.id

/**
 * Utility function to automatically refresh tiles after a change of layers.
 */
private fun MapState.setLayer(layer: LayerFactory?) {
    tileCanvasState.setLayer(layer)
    renderVisibleTilesThrottled()
}
