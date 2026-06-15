package ovh.plrapps.mapcompose.core

import java.util.UUID

internal data class LayerFactory(
    @JvmField val id: String,
    @JvmField val alpha: Float = 1f,
    @JvmField val tileBitmapProvider: TileBitmapProvider
)

/**
 * use arrays instead of boxed lists to reduce memory usage
 */
internal class CompliedLayers(
    factories: Array<LayerFactory> // no `val` to avoid `this`
) {
    @JvmField
    val factories: Array<LayerFactory>

    @JvmField
    val layerIds: Array<String>

    @JvmField
    val opacities: FloatArray

    init {
        val opacities = FloatArray(factories.size)
        this.layerIds = Array(factories.size) {
            val factory = factories[it]
            opacities[it] = factory.alpha
            factory.id
        }
        this.opacities = opacities
        this.factories = factories
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.factories.contentEquals((other as CompliedLayers).factories)
    }

    override fun hashCode(): Int = this.factories.contentHashCode()
}

sealed interface LayerPlacement
data object AboveAll : LayerPlacement
data object BelowAll : LayerPlacement
data class AboveLayer(val layerId: String) : LayerPlacement
data class BelowLayer(val layerId: String) : LayerPlacement

internal fun makeLayerId(): String = UUID.randomUUID().toString()
