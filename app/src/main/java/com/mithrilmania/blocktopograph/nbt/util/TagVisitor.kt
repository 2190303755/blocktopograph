package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag

interface TagVisitor {
    fun visit(tag: StringTag)
    fun visit(tag: ByteTag)
    fun visit(tag: ShortTag)
    fun visit(tag: IntTag)
    fun visit(tag: LongTag)
    fun visit(tag: FloatTag)
    fun visit(tag: DoubleTag)
    fun visit(tag: ByteArrayTag)
    fun visit(tag: IntArrayTag)
    fun visit(tag: LongArrayTag)
    fun visit(tag: ListTag)
    fun visit(tag: CompoundTag)
}