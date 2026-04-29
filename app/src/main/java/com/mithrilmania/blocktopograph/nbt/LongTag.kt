package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toLongTag
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class LongTag(@JvmField val value: Long) : NumericTag {
    override val type: Type get() = Type
    override fun toNumber(): Long = this.value
    override fun toByte(): Byte = this.value.toByte()
    override fun toShort(): Short = this.value.toShort()
    override fun toInt(): Int = this.value.toInt()
    override fun toLong(): Long = this.value
    override fun toFloat(): Float = this.value.toFloat()
    override fun toDouble(): Double = this.value.toDouble()
    override fun toString(): String = this.value.toString()
    override fun write(output: DataOutput) {
        output.writeLong(this.value)
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    companion object Type : NumericTagType<LongTag> {
        const val PAYLOAD_SIZE: Int = 8
        override val typeId get() = TAG_LONG
        override fun toString() = "TAG_Long"
        override fun read(input: DataInput, depth: Int) = LongTag(input.readLong())
        override fun transform(tag: BinaryTag) = tag.toLongTag()
    }
}