package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class DoubleTag(override val value: Double) : NumericTag<Double> {
    override val type get() = Type
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = this
    override fun getAsByte() = this.value.toInt().toByte()
    override fun getAsShort() = this.value.toInt().toShort()
    override fun getAsInt() = this.value.toInt()
    override fun getAsLong() = this.value.toLong()
    override fun getAsFloat() = this.value.toFloat()
    override fun getAsDouble() = this.value
    override fun write(output: DataOutput) {
        output.writeDouble(this.value)
    }

    companion object Type : TagType<DoubleTag> {
        const val SIZE = 8
        override val id get() = TAG_DOUBLE
        override fun toString() = "TAG_Double"
        override fun read(input: DataInput, depth: Int) = DoubleTag(input.readDouble())
    }
}