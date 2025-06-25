package com.mithrilmania.blocktopograph.nbt.io

import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag

sealed interface NBTResult {
    val tag: BinaryTag<*>
}

@JvmInline
value class SNBTResult(override val tag: CompoundTag) : NBTResult

class NamedResult(
    val name: String,
    override val tag: BinaryTag<*>,
    val compressed: Boolean = false,
    val version: UInt? = null,
    val littleEndian: Boolean = true
) : NBTResult