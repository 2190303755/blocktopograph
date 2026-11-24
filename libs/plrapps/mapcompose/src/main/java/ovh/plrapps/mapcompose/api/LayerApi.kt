@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.LayerFactory
import ovh.plrapps.mapcompose.core.TileBitmapProvider
import ovh.plrapps.mapcompose.ui.state.MapModel

fun MapModel.setLayer(name: String, layer: TileBitmapProvider) {
    setLayer(LayerFactory(name, layer))
}

fun MapModel.clearLayer() {
    setLayer(null)
}

val MapModel.layerId: String?
    get() = tileCanvasState.layer.value?.id

/**
 * Utility function to automatically refresh tiles after a change of layers.
 */
private fun MapModel.setLayer(layer: LayerFactory?) {
    tileCanvasState.layer.value = layer
    renderVisibleTilesThrottled()
}
