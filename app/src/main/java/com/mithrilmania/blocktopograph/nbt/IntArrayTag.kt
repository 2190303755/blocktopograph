package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.util.add
import com.mithrilmania.blocktopograph.util.removeAt
import java.io.DataInput
import java.io.DataOutput

class IntArrayTag(var array: IntArray) : ArrayTag<IntTag>() {
    override val type get() = Type
    override val value get() = this
    override val elementType get() = TAG_INT
    override val size get() = this.array.size
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = IntArrayTag(this.array.copyOf())
    override fun get(index: Int) = IntTag(this.array[index])
    override fun write(output: DataOutput) {
        output.writeInt(this.size)
        this.array.forEach(output::writeInt)
    }

    override fun setTag(index: Int, tag: BinaryTag<*>): Boolean {
        if (tag is NumericTag) {
            this.array[index] = tag.getAsInt()
            return true
        }
        return false
    }

    override fun addTag(index: Int, tag: BinaryTag<*>): Boolean {
        if (tag is NumericTag) {
            this.array = this.array.add(index, tag.getAsInt())
            return true
        }
        return false
    }

    override fun removeAt(index: Int): IntTag {
        val value = this.array[index]
        this.array = this.array.removeAt(index)
        return IntTag(value)
    }

    override fun clear() {
        this.array = IntArray(0)
    }

    companion object Type : TagType<IntArrayTag> {
        override val id get() = TAG_INT_ARRAY
        override fun toString() = "TAG_Int_Array"
        override fun read(input: DataInput, depth: Int) = IntArrayTag(
            IntArray(input.readInt()) { input.readInt() }
        )
    }
}