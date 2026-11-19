package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * A [Tile] is defined by its coordinates in the "pyramid".
 */
internal data class Tile(
    @JvmField val zoom: Int,
    @JvmField val row: Int,
    @JvmField val col: Int,
    @JvmField val layerId: String
) {
    @JvmField
    @Volatile
    var bitmap: Bitmap? = null   // write on main-thread only

    var alpha: Float by mutableFloatStateOf(0f)
}

internal fun Tile.spaceKey() = SpaceKey(zoom, row, col)

internal data class SpaceKey(
    @JvmField val zoom: Int,
    @JvmField val row: Int,
    @JvmField val col: Int
)

internal typealias TileSpec = SpaceKey
