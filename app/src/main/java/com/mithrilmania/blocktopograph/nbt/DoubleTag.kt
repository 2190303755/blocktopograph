package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toDoubleTag
import java.io.DataInput
import java.io.DataOutput
import kotlin.math.floor

@JvmInline
value class DoubleTag(@JvmField val value: Double) : NumericTag {
    override val type: Type get() = Type
    override fun toNumber(): Double = this.value
    override fun toByte(): Byte = this.toInt().toByte()
    override fun toShort(): Short = this.toInt().toShort()
    override fun toInt(): Int = floor(this.value).toInt()
    override fun toLong(): Long = floor(this.value).toLong()
    override fun toFloat(): Float = this.value.toFloat()
    override fun toDouble(): Double = this.value
    override fun toString(): String = this.value.toString()
    override fun write(output: DataOutput) {
        output.writeDouble(this.value)
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    companion object Type : NumericTagType<DoubleTag> {
        const val PAYLOAD_SIZE: Int = 8
        override val typeId get() = TAG_DOUBLE
        override fun toString() = "TAG_Double"
        override fun read(input: DataInput, depth: Int) = DoubleTag(input.readDouble())
        override fun transform(tag: BinaryTag) = tag.toDoubleTag()
    }
}