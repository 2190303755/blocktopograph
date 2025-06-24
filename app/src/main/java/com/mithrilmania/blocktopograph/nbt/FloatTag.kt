package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class FloatTag(override val value: Float) : NumericTag<Float> {
    override val type get() = Type
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = this
    override fun getAsByte() = this.value.toInt().toByte()
    override fun getAsShort() = this.value.toInt().toShort()
    override fun getAsInt() = this.value.toInt()
    override fun getAsLong() = this.value.toLong()
    override fun getAsFloat() = this.value
    override fun getAsDouble() = this.value.toDouble()
    override fun write(output: DataOutput) {
        output.writeFloat(this.value)
    }

    companion object Type : TagType<FloatTag> {
        const val SIZE = 4
        override val id get() = TAG_FLOAT
        override fun toString() = "TAG_Float"
        override fun read(input: DataInput, depth: Int) = FloatTag(input.readFloat())
    }
}