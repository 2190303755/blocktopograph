package ovh.plrapps.mapcompose.core

internal data class LayerFactory(
    @JvmField val id: String,
    @JvmField val tileBitmapProvider: TileBitmapProvider
)
