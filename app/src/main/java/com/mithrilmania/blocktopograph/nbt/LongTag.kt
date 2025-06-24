package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class LongTag(override val value: Long) : NumericTag<Long> {
    override val type get() = Type
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = this
    override fun getAsByte() = this.value.toByte()
    override fun getAsShort() = this.value.toShort()
    override fun getAsInt() = this.value.toInt()
    override fun getAsLong() = this.value
    override fun getAsFloat() = this.value.toFloat()
    override fun getAsDouble() = this.value.toDouble()
    override fun write(output: DataOutput) {
        output.writeLong(this.value)
    }

    companion object Type : TagType<LongTag> {
        const val SIZE = 8
        override val id get() = TAG_LONG
        override fun toString() = "TAG_Long"
        override fun read(input: DataInput, depth: Int) = LongTag(input.readLong())
    }
}