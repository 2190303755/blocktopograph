package ovh.plrapps.mapcompose.core

import androidx.annotation.AnyThread
import androidx.compose.ui.util.fastCoerceAtMost
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2

/**
 * Resolves the visible tiles.
 * This class is immutable and thread-safe, so its methods may be invoked from any thread.
 *
 * @param levelCount Number of levels
 * @param magnifyingFactor Alters the level at which tiles are picked for a given scale. By default,
 * the level immediately higher (in index) is picked, to avoid sub-sampling. This corresponds to a
 * [magnifyingFactor] of 0. The value 1 will result in picking the current level at a given scale,
 * which will be at a relative scale between 1.0 and 2.0
 *
 * @author p-lr on 25/05/2019
 */
internal class VisibleTilesResolver(
    levelCount: Int,
    @JvmField val tileSize: Int = 256,
    @JvmField val magnifyingFactor: Int = 0
) {
    @JvmField
    val maxLevel: Int = levelCount - 1

    /**
     * Last level is at scale 1.0, others are at scale 1.0 / power_of_2
     */
    fun scaleShiftAtLevel(level: Int): Int = maxLevel - level

    /**
     * Returns the level, an entire value belonging to [0 ; [maxLevel]]
     */
    internal fun getLevel(scale: Double, magnifyingFactor: Int): Int {
        /* This value can be negative */
        val partialLevel = log2(scale) - magnifyingFactor

        /* The level can't be greater than maxLevel */
        val capedLevel = maxLevel + partialLevel.fastCoerceAtMost(0.0)

        /* The level can't be lower than 0 */
        return if (capedLevel > 0.0) ceil(capedLevel).toInt() else 0
    }

    /**
     * Resolves the visible tiles from the pixel-space bounds of the viewport.
     * Values are in pixels and depend on [scale].
     */
    @AnyThread
    internal fun getVisibleTiles(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        scale: Double
    ): VisibleTiles {
        val level = getLevel(scale, this.magnifyingFactor)

        val scaledTileSize = (this.tileSize shl scaleShiftAtLevel(level)) * scale
        val sizeFactor = 1.0 / scaledTileSize

        return VisibleTiles(
            level,
            floor(left * sizeFactor).toInt(),
            floor(top * sizeFactor).toInt(),
            ceil(right * sizeFactor).toInt() - 1,
            ceil(bottom * sizeFactor).toInt() - 1,
        )
    }
}
