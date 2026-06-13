package com.mithrilmania.blocktopograph.world

import androidx.annotation.IntRange

/**
 * @see androidx.collection.IntIntPair
 */
@JvmInline
value class HeightRange(@JvmField val packed: Int) {
    constructor(min: Short, max: Short) : this((min.toInt() shl 16) or (max.toInt() and 0xFFFF))
    constructor(
        @IntRange(from = Short.MIN_VALUE.toLong(), to = Short.MAX_VALUE.toLong()) min: Int,
        @IntRange(from = Short.MIN_VALUE.toLong(), to = Short.MAX_VALUE.toLong()) max: Int
    ) : this((min shl 16) or (max and 0xFFFF))

    val min: Short get() = (this.packed shr 16).toShort()
    val max: Short get() = this.packed.toShort()
}