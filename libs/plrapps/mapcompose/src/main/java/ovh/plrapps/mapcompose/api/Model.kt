package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.utils.Point

typealias Camera = Point

data class BoundingBox(
    @JvmField val xLeft: Double,
    @JvmField val yTop: Double,
    @JvmField val xRight: Double,
    @JvmField val yBottom: Double
)

/**
 * ```
 *    p1         p2
 *      ---------
 *      |       |
 *      |       |
 *      ---------
 *    p4         p3
 * ```
 */
typealias VisibleArea = BoundingBox

val VisibleArea.p1x: Double
    get() = xLeft
val VisibleArea.p1y: Double
    get() = yTop
val VisibleArea.p2x: Double
    get() = xRight
val VisibleArea.p2y: Double
    get() = yTop
val VisibleArea.p3x: Double
    get() = xRight
val VisibleArea.p3y: Double
    get() = yBottom
val VisibleArea.p4x: Double
    get() = xLeft
val VisibleArea.p4y: Double
    get() = yBottom
