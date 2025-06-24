package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.util.add
import com.mithrilmania.blocktopograph.util.removeAt
import java.io.DataInput
import java.io.DataOutput

class ByteArrayTag(var array: ByteArray) : ArrayTag<ByteTag>() {
    override val type get() = Type
    override val value get() = this
    override val elementType get() = TAG_BYTE
    override val size get() = this.array.size
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = ByteArrayTag(this.array.copyOf())
    override fun get(index: Int) = ByteTag(this.array[index])
    override fun write(output: DataOutput) {
        output.writeInt(this.size)
        output.write(this.array)
    }

    override fun setTag(index: Int, tag: BinaryTag<*>): Boolean {
        if (tag is NumericTag) {
            this.array[index] = tag.getAsByte()
            return true
        }
        return false
    }

    override fun addTag(index: Int, tag: BinaryTag<*>): Boolean {
        if (tag is NumericTag) {
            this.array = this.array.add(index, tag.getAsByte())
            return true
        }
        return false
    }

    override fun removeAt(index: Int): ByteTag {
        val value = this.array[index]
        this.array = this.array.removeAt(index)
        return ByteTag(value)
    }

    override fun clear() {
        this.array = ByteArray(0)
    }

    companion object Type : TagType<ByteArrayTag> {
        override val id get() = TAG_BYTE_ARRAY
        override fun toString() = "TAG_Byte_Array"
        override fun read(input: DataInput, depth: Int) = ByteArrayTag(
            ByteArray(input.readInt()).also { input.readFully(it) }
        )
    }
}