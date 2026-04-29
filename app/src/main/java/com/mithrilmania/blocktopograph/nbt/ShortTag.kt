package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toShortTag
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class ShortTag(@JvmField val value: Short) : NumericTag {
    constructor(tag: NumericTag) : this(tag.toShort())
    override val type: Type get() = Type
    override fun toNumber(): Short = this.value
    override fun toByte(): Byte = this.value.toByte()
    override fun toShort(): Short = this.value
    override fun toInt(): Int = this.value.toInt()
    override fun toLong(): Long = this.value.toLong()
    override fun toFloat(): Float = this.value.toFloat()
    override fun toDouble(): Double = this.value.toDouble()
    override fun toString(): String = this.value.toString()
    override fun write(output: DataOutput) {
        output.writeShort(this.value.toInt())
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    companion object Type : NumericTagType<ShortTag> {
        const val PAYLOAD_SIZE: Int = 2
        override val typeId get() = TAG_SHORT
        override fun toString() = "TAG_Short"
        override fun read(input: DataInput, depth: Int) = ShortTag(input.readShort())
        override fun transform(tag: BinaryTag) = tag.toShortTag()
    }
}