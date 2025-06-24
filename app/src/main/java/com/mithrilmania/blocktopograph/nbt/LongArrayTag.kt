package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.util.add
import com.mithrilmania.blocktopograph.util.removeAt
import java.io.DataInput
import java.io.DataOutput

class LongArrayTag(var array: LongArray) : ArrayTag<LongTag>() {
    override val type get() = Type
    override val value get() = this
    override val elementType get() = TAG_LONG
    override val size get() = this.array.size
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = LongArrayTag(this.array.copyOf())
    override fun get(index: Int) = LongTag(this.array[index])
    override fun write(output: DataOutput) {
        output.writeInt(this.size)
        this.array.forEach(output::writeLong)
    }

    override fun setTag(index: Int, tag: BinaryTag<*>): Boolean {
        if (tag is NumericTag) {
            this.array[index] = tag.getAsLong()
            return true
        }
        return false
    }

    override fun addTag(index: Int, tag: BinaryTag<*>): Boolean {
        if (tag is NumericTag) {
            this.array = this.array.add(index, tag.getAsLong())
            return true
        }
        return false
    }

    override fun removeAt(index: Int): LongTag {
        val value = this.array[index]
        this.array = this.array.removeAt(index)
        return LongTag(value)
    }

    override fun clear() {
        this.array = LongArray(0)
    }

    companion object Type : TagType<LongArrayTag> {
        override val id get() = TAG_LONG_ARRAY
        override fun toString() = "TAG_Long_Array"
        override fun read(input: DataInput, depth: Int) = LongArrayTag(
            LongArray(input.readInt()) { input.readLong() }
        )
    }
}