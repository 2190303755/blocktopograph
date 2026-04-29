package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toIntTag
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class IntTag(@JvmField val value: Int) : NumericTag {
    override val type: Type get() = Type
    override fun toNumber(): Int = this.value
    override fun toByte(): Byte = this.value.toByte()
    override fun toShort(): Short = this.value.toShort()
    override fun toInt(): Int = this.value
    override fun toLong(): Long = this.value.toLong()
    override fun toFloat(): Float = this.value.toFloat()
    override fun toDouble(): Double = this.value.toDouble()
    override fun toString(): String = this.value.toString()
    override fun write(output: DataOutput) {
        output.writeInt(this.value)
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    companion object Type : NumericTagType<IntTag> {
        const val PAYLOAD_SIZE: Int = 4
        override val typeId get() = TAG_INT
        override fun toString() = "TAG_Int"
        override fun read(input: DataInput, depth: Int) = IntTag(input.readInt())
        override fun transform(tag: BinaryTag) = tag.toIntTag()
    }
}