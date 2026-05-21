package ovh.plrapps.mapcompose.core

import java.util.UUID

internal data class LayerFactory(
    val id: String,
    val tileBitmapProvider: TileBitmapProvider,
    val alpha: Float = 1f
)

sealed interface LayerPlacement
data object AboveAll : LayerPlacement
data object BelowAll : LayerPlacement
data class AboveLayer(val layerId: String) : LayerPlacement
data class BelowLayer(val layerId: String) : LayerPlacement

internal fun makeLayerId(): String = UUID.randomUUID().toString()
