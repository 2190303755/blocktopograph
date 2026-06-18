package ovh.plrapps.mapcompose.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Resolves the visible tiles.
 * This class isn't thread-safe, and public methods should be invoked from the same thread to ensure
 * consistency.
 *
 * @param levelCount Number of levels
 * @param magnifyingFactor Alters the level at which tiles are picked for a given scale. By default,
 * the level immediately higher (in index) is picked, to avoid sub-sampling. This corresponds to a
 * [magnifyingFactor] of 0. The value 1 will result in picking the current level at a given scale,
 * which will be at a relative scale between 1.0 and 2.0
 * @param scaleProvider Since the component which invokes [getVisibleTiles] isn't likely to be the
 * component which owns the scale state, we provide it here as a loosely coupled reference.
 *
 * @author p-lr on 25/05/2019
 */
internal class VisibleTilesResolver(
    private val levelCount: Int,
    private val tileSize: Int = 256,
    var magnifyingFactor: Int = 0,
    private val scaleProvider: ScaleProvider,
) {

    private val scaleFactorForLevel: IntArray = IntArray(
        levelCount,
        this::scaleFactorForLevel
    )

    /**
     * Last level is at scale 1.0, others are at scale 1.0 / power_of_2
     */
    fun scaleFactorForLevel(level: Int): Int {
        return 1 shl (levelCount - level - 1)
    }

    /**
     * @return the scale factor for a given [level] (also called zoom)
     */
    fun getScaleFactorForLevel(level: Int): Int {
        return scaleFactorForLevel.getOrElse(level, this::scaleFactorForLevel)
    }

    /**
     * Returns the level, an entire value belonging to [0 ; [levelCount] - 1]
     */
    internal fun getLevel(scale: Double, magnifyingFactor: Int = 0): Int {
        /* This value can be negative */
        val partialLevel = levelCount - 1 - magnifyingFactor +
                ln(scale) / ln(2.0)

        /* The level can't be greater than levelCount - 1.0 */
        val capedLevel = min(partialLevel, levelCount - 1.0)

        /* The level can't be lower than 0 */
        return ceil(max(capedLevel, 0.0)).toInt()
    }

    /**
     * Get the [VisibleTiles], given the visible area in pixels.
     *
     * @param viewport The [Viewport] which represents the visible area. Its values depend on the
     * scale.
     */
    fun getVisibleTiles(viewport: Viewport): VisibleTiles {
        val scale = scaleProvider.getScale()
        val level = getLevel(scale, magnifyingFactor)
        val scaleFactorAtLevel = getScaleFactorForLevel(level)

        val scaledTileSize = tileSize * scaleFactorAtLevel * scale
        val sizeFactor = 1.0 / scaledTileSize

        val colLeft = floor(viewport.left * sizeFactor).toInt()
        val rowTop = floor(viewport.top * sizeFactor).toInt()
        val colRight = (ceil(viewport.right * sizeFactor).toInt() - 1)
        val rowBottom = (ceil(viewport.bottom * sizeFactor).toInt() - 1)

        return VisibleTiles(
            level,
            colLeft,
            rowTop,
            colRight,
            rowBottom,
            getSubSample(scale)
        )
    }

    // internal for test purposes
    internal fun getSubSample(scale: Double): Int {
        val max = 1.0 / getScaleFactorForLevel(0)
        return if (scale < max) {
            ceil(ln(max / scale) / ln(2.0)).toInt()
        } else {
            0
        }
    }

    fun interface ScaleProvider {
        fun getScale(): Double
    }
}

/**
 * Properties container for the computed visible tiles.
 * @param level 0-based level index
 * TODO doc
 * @param subSample the current sub-sample factor. If the current scale of the [VisibleTilesResolver]
 * is lower than the scale of the minimum level, [subSample] is greater than 0. Otherwise, [subSample]
 * equals 0.
 */
internal data class VisibleTiles(
    @JvmField val level: Int,
    @JvmField val left: Int,
    @JvmField val top: Int,
    @JvmField val right: Int,
    @JvmField val bottom: Int,
    @JvmField val subSample: Int = 0
)

internal fun VisibleTiles.contains(tile: Tile): Boolean {
    return level == tile.zoom && subSample == tile.subSample
            && tile.row in this.top..this.bottom
            && tile.col in this.left..this.right
}

internal fun VisibleTiles.intersects(tile: Tile): Boolean {
    return if (level == tile.zoom) {
        tile.row in this.top..this.bottom && tile.col in this.left..this.right
    } else {
        val curMinRow = this.top
        val curMaxRow = this.bottom
        val curMinCol = this.left
        val curMaxCol = this.right

        if (tile.zoom > level) { // User is zooming out
            val dLevel = tile.zoom - level
            val minRowAtLvl = curMinRow.minAtGreaterLevel(dLevel)
            val maxRowAtLvl = curMaxRow.maxAtGreaterLevel(dLevel)

            val minColAtLvl = curMinCol.minAtGreaterLevel(dLevel)
            val maxColAtLvl = curMaxCol.maxAtGreaterLevel(dLevel)
            tile.row in minRowAtLvl..maxRowAtLvl && tile.col in minColAtLvl..maxColAtLvl
        } else { // User is zooming in
            val dLevel = level - tile.zoom
            val minRowAtLvl = tile.row.minAtGreaterLevel(dLevel)
            val maxRowAtLvl = tile.row.maxAtGreaterLevel(dLevel)

            val minColAtLvl = tile.col.minAtGreaterLevel(dLevel)
            val maxColAtLvl = tile.col.maxAtGreaterLevel(dLevel)
            curMinCol <= maxColAtLvl && minColAtLvl <= curMaxCol && curMinRow <= maxRowAtLvl &&
                    minRowAtLvl <= curMaxRow
        }
    }
}

private fun Int.minAtGreaterLevel(n: Int): Int {
    return this shl n
}

private fun Int.maxAtGreaterLevel(n: Int): Int {
    return ((this + 1) shl n) - 1
}
