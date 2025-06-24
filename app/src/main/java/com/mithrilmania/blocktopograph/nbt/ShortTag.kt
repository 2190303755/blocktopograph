package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class ShortTag(override val value: Short) : NumericTag<Short> {
    override val type get() = Type
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = this
    override fun getAsByte() = this.value.toByte()
    override fun getAsShort() = this.value
    override fun getAsInt() = this.value.toInt()
    override fun getAsLong() = this.value.toLong()
    override fun getAsFloat() = this.value.toFloat()
    override fun getAsDouble() = this.value.toDouble()
    override fun write(output: DataOutput) {
        output.writeShort(this.value.toInt())
    }

    companion object Type : TagType<ShortTag> {
        const val SIZE = 2
        override val id get() = TAG_SHORT
        override fun toString() = "TAG_Short"
        override fun read(input: DataInput, depth: Int) = ShortTag(input.readShort())
    }
}