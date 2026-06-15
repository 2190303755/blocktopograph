package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * A [Tile] is defined by its coordinates in the "pyramid". A [Tile] is sub-sampled when the
 * scale becomes lower than the scale of the lowest level. To reflect that, there is [subSample]
 * property which is a positive integer (can be 0). When [subSample] equals 0, the [bitmap] of the
 * tile is full scale. When [subSample] equals 1, the [bitmap] is sub-sampled and its size is half
 * the original bitmap (the one at the lowest level), and so on.
 */
internal data class Tile(
    val zoom: Int,
    val row: Int,
    val col: Int,
    val subSample: Int,
    val layerIds: Array<String>,
    val opacities: FloatArray
) {
    @Volatile
    var bitmap: Bitmap? = null   // write on main-thread only

    var alpha: Float by mutableFloatStateOf(0f)

    @Volatile
    var overlaps: Tile? = null

    @Volatile
    var markedForSweep = false   // write on main-thread only

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Tile
        return zoom == other.zoom
                && row == other.row
                && col == other.col
                && subSample == other.subSample
                && rendererEquals(other.layerIds, other.opacities)
    }

    override fun hashCode(): Int {
        var result = zoom
        result = 31 * result + row
        result = 31 * result + col
        result = 31 * result + subSample
        result = 31 * result + markedForSweep.hashCode()
        result = 31 * result + layerIds.contentHashCode()
        result = 31 * result + opacities.contentHashCode()
        return result
    }
}

internal fun Tile.rendererEquals(layerIds: Array<String>, opacities: FloatArray): Boolean {
    return this.layerIds.contentEquals(layerIds) && this.opacities.contentEquals(opacities)
}

internal data class TileSpec(
    val zoom: Int,
    val row: Int,
    val col: Int,
    val subSample: Int = 0
)

internal fun Tile.spaceKey(): SpaceKey {
    return "row=$row,col=$col,zoom=$zoom"
}

internal typealias SpaceKey = String
