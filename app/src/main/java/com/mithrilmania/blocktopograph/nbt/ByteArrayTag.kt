package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toByteTag
import com.mithrilmania.blocktopograph.util.expandAt
import com.mithrilmania.blocktopograph.util.shrinkAt
import java.io.DataInput
import java.io.DataOutput

data class ByteArrayTag(
    @JvmField
    var elements: ByteArray = ByteArray(0)
) : ArrayTag<ByteTag> {
    override val elementTypeId: Byte get() = TAG_BYTE
    override val type: Type get() = Type
    override val size: Int get() = this.elements.size
    operator fun get(index: Int): Byte = this.elements[index]
    override fun getAsLong(index: Int): Long = this[index].toLong()
    override fun getAsTag(index: Int): ByteTag = ByteTag(this[index])

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    override fun write(output: DataOutput) {
        output.writeInt(this.size)
        output.write(this.elements)
    }

    operator fun set(index: Int, value: Byte) {
        this.elements[index] = value
    }

    override fun setTag(index: Int, tag: BinaryTag): Boolean {
        if (tag is NumericTag) {
            this[index] = tag.toByte()
            return true
        }
        return false
    }

    override fun addTag(index: Int, tag: BinaryTag): Boolean {
        if (tag is NumericTag) {
            this.elements = this.elements.expandAt(index, ::ByteArray)
            this.elements[index] = tag.toByte()
            return true
        }
        return false
    }

    override fun clear() {
        this.elements = ByteArray(0)
    }

    fun remove(index: Int): Byte {
        val value = this.elements[index]
        this.elements = this.elements.shrinkAt(index, ::ByteArray)
        return value
    }

    override fun removeTag(index: Int): ByteTag = ByteTag(this.remove(index))

    override fun copy(): ByteArrayTag = ByteArrayTag(this.elements.copyOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.elements.contentEquals((other as ByteArrayTag).elements)
    }

    override fun hashCode(): Int = this.elements.contentHashCode()

    companion object Type : TagType<ByteArrayTag> {
        override val typeId get() = TAG_BYTE_ARRAY
        override fun toString() = "TAG_Byte_Array"
        override fun read(input: DataInput, depth: Int) = ByteArrayTag(
            ByteArray(input.readInt()).also { input.readFully(it) }
        )

        override fun transform(tag: BinaryTag) = when (tag) {
            is NumericTag -> ByteArrayTag(byteArrayOf(tag.toByte()))
            is StringTag -> ByteArrayTag(byteArrayOf(tag.value.toByteOrNull() ?: 0.toByte()))
            is ByteArrayTag -> tag
            is IntArrayTag -> ByteArrayTag(ByteArray(tag.size) { tag[it].toByte() })
            is LongArrayTag -> ByteArrayTag(ByteArray(tag.size) { tag[it].toByte() })
            is ListTag -> ByteArrayTag(ByteArray(tag.size) { tag[it].toByteTag().value })
            is CompoundTag -> {
                val elements = ByteArray(tag.size)
                var index = 0
                tag.forEachSorted {
                    elements[index++] = it.toByteTag().value
                }
                ByteArrayTag(elements)
            }

            else -> ByteArrayTag()
        }
    }
}