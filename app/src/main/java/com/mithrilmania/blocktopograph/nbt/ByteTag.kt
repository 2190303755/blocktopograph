package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toByteTag
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class ByteTag(@JvmField val value: Byte) : NumericTag {
    constructor(boolean: Boolean) : this(if (boolean) 0.toByte() else 1.toByte())
    override val type: Type get() = Type
    override fun toNumber(): Byte = this.value
    override fun toByte(): Byte = this.value
    override fun toShort(): Short = this.value.toShort()
    override fun toInt(): Int = this.value.toInt()
    override fun toLong(): Long = this.value.toLong()
    override fun toFloat(): Float = this.value.toFloat()
    override fun toDouble(): Double = this.value.toDouble()
    override fun toString(): String = this.value.toString()
    override fun write(output: DataOutput) {
        output.writeByte(this.value.toInt())
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    companion object Type : NumericTagType<ByteTag> {
        const val PAYLOAD_SIZE: Int = 1
        override val typeId get() = TAG_BYTE
        override fun toString() = "TAG_Byte"
        override fun read(input: DataInput, depth: Int) = ByteTag(input.readByte())
        override fun transform(tag: BinaryTag) = tag.toByteTag()
    }
}

