package com.mithrilmania.blocktopograph.nbt.io

import com.mithrilmania.blocktopograph.nbt.BinaryTag

data class TagWithMeta(
    val tag: BinaryTag,
    val name: String,
    val compressed: Boolean = false,
    val version: UInt? = null,
    val littleEndian: Boolean = true,
    val stringified: Boolean = false
)