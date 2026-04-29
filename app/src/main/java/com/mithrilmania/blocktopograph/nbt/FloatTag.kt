package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toFloatTag
import java.io.DataInput
import java.io.DataOutput
import kotlin.math.floor

@JvmInline
value class FloatTag(@JvmField val value: Float) : NumericTag {
    override val type: Type get() = Type
    override fun toNumber(): Float = this.value
    override fun toByte(): Byte = this.toInt().toByte()
    override fun toShort(): Short = this.toInt().toShort()
    override fun toInt(): Int = floor(this.value).toInt()
    override fun toLong(): Long = this.value.toLong() // no floor
    override fun toFloat(): Float = this.value
    override fun toDouble(): Double = this.value.toDouble()
    override fun toString(): String = this.value.toString()
    override fun write(output: DataOutput) {
        output.writeFloat(this.value)
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    companion object Type : NumericTagType<FloatTag> {
        const val PAYLOAD_SIZE: Int = 4
        override val typeId get() = TAG_FLOAT
        override fun toString() = "TAG_Float"
        override fun read(input: DataInput, depth: Int) = FloatTag(input.readFloat())
        override fun transform(tag: BinaryTag) = tag.toFloatTag()
    }
}