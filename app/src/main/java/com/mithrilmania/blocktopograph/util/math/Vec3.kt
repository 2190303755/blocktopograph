package com.mithrilmania.blocktopograph.util.math

data class DimensionVec3i(
    @JvmField val dimensionId: Int,
    @JvmField val x: Int,
    @JvmField val y: Int,
    @JvmField val z: Int
)

data class DimensionVec3f(
    @JvmField val dimensionId: Int,
    @JvmField val x: Float,
    @JvmField val y: Float,
    @JvmField val z: Float
)
