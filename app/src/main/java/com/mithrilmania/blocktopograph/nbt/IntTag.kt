package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class IntTag(override val value: Int) : NumericTag<Int> {
    override val type get() = Type
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = this
    override fun getAsByte() = this.value.toByte()
    override fun getAsShort() = this.value.toShort()
    override fun getAsInt() = this.value
    override fun getAsLong() = this.value.toLong()
    override fun getAsFloat() = this.value.toFloat()
    override fun getAsDouble() = this.value.toDouble()
    override fun write(output: DataOutput) {
        output.writeInt(this.value)
    }

    companion object Type : TagType<IntTag> {
        const val SIZE = 4
        override val id get() = TAG_INT
        override fun toString() = "TAG_Int"
        override fun read(input: DataInput, depth: Int) = IntTag(input.readInt())
    }
}