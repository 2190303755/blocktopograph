package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * A [Tile] is defined by its coordinates in the "pyramid".
 */
internal data class Tile(
    val zoom: Int,
    val row: Int,
    val col: Int,
    val layerId: String
) {
    @Volatile
    var bitmap: Bitmap? = null   // write on main-thread only

    var alpha: Float by mutableFloatStateOf(0f)

    @Volatile
    var overlaps: Tile? = null

    @Volatile
    var markedForSweep = false   // write on main-thread only
}

internal fun Tile.spaceKey() = SpaceKey(zoom, row, col)

internal data class SpaceKey(
    @JvmField val zoom: Int,
    @JvmField val row: Int,
    @JvmField val col: Int
)

internal typealias TileSpec = SpaceKey
