package ovh.plrapps.mapcompose.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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

    /**
     * Last level is at scale 1.0, others are at scale 1.0 / power_of_2
     */
    private val scaleForLevel: DoubleArray = DoubleArray(
        levelCount,
        this::calculateScaleForLevel
    )

    fun calculateScaleForLevel(level: Int): Double {
        return 0.5.pow((levelCount - level - 1))
    }

    /**
     * Get the scale for a given [level] (also called zoom).
     * @return the scale or null if no such level was configured.
     */
    fun getScaleForLevel(level: Int): Double {
        return scaleForLevel.getOrElse(level, this::calculateScaleForLevel)
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
        val scaleAtLevel = getScaleForLevel(level)
        val relativeScale = scale / scaleAtLevel

        val scaledTileSize = tileSize.toDouble() * relativeScale

        val colLeft = floor(viewport.left / scaledTileSize).toInt()
        val rowTop = floor(viewport.top / scaledTileSize).toInt()
        val colRight = (ceil(viewport.right / scaledTileSize).toInt() - 1)
        val rowBottom = (ceil(viewport.bottom / scaledTileSize).toInt() - 1)
        val tileMatrix = (rowTop..rowBottom).associateWith {
            colLeft..colRight
        }

        return VisibleTiles(level, tileMatrix, getSubSample(scale))
    }

    // internal for test purposes
    internal fun getSubSample(scale: Double): Int {
        val max = getScaleForLevel(0)
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


internal typealias Row = Int
internal typealias ColRange = IntRange

/* Contains all (row, col) indexes, grouped by rows*/
internal typealias TileMatrix = Map<Row, ColRange>

/**
 * Properties container for the computed visible tiles.
 * @param level 0-based level index
 * @param tileMatrix contains information about which tiles are currently visible
 * @param subSample the current sub-sample factor. If the current scale of the [VisibleTilesResolver]
 * is lower than the scale of the minimum level, [subSample] is greater than 0. Otherwise, [subSample]
 * equals 0.
 */
internal data class VisibleTiles(
    val level: Int,
    val tileMatrix: TileMatrix,
    val subSample: Int = 0
)
