package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class ByteTag internal constructor(override val value: Byte) : NumericTag<Byte> {
    override val type get() = Type
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = this
    override fun getAsByte() = this.value
    override fun getAsInt() = this.value.toInt()
    override fun getAsShort() = this.value.toShort()
    override fun getAsLong() = this.value.toLong()
    override fun getAsFloat() = this.value.toFloat()
    override fun getAsDouble() = this.value.toDouble()
    override fun write(output: DataOutput) {
        output.writeByte(this.value.toInt())
    }

    companion object Type : TagType<ByteTag> {
        const val SIZE = 1
        override val id get() = TAG_BYTE
        override fun toString() = "TAG_Byte"
        override fun read(input: DataInput, depth: Int) = ByteTag(input.readByte())
    }
}