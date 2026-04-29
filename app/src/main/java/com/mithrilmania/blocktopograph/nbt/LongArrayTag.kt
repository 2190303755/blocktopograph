package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toLongTag
import com.mithrilmania.blocktopograph.util.expandAt
import com.mithrilmania.blocktopograph.util.shrinkAt
import java.io.DataInput
import java.io.DataOutput

data class LongArrayTag(
    @JvmField
    var elements: LongArray = LongArray(0)
) : ArrayTag<LongTag> {
    override val elementTypeId: Byte get() = TAG_LONG
    override val type: Type get() = Type
    override val size: Int get() = this.elements.size
    operator fun get(index: Int): Long = this.elements[index]
    override fun getAsLong(index: Int): Long = this[index]
    override fun getAsTag(index: Int): LongTag = LongTag(this[index])
    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    override fun write(output: DataOutput) {
        output.writeInt(this.size)
        this.elements.forEach(output::writeLong)
    }

    operator fun set(index: Int, value: Long) {
        this.elements[index] = value
    }

    override fun setTag(index: Int, tag: BinaryTag): Boolean {
        if (tag is NumericTag) {
            this[index] = tag.toLong()
            return true
        }
        return false
    }

    override fun addTag(index: Int, tag: BinaryTag): Boolean {
        if (tag is NumericTag) {
            this.elements = this.elements.expandAt(index, ::LongArray)
            this.elements[index] = tag.toLong()
            return true
        }
        return false
    }

    override fun clear() {
        this.elements = LongArray(0)
    }

    fun remove(index: Int): Long {
        val value = this.elements[index]
        this.elements = this.elements.shrinkAt(index, ::LongArray)
        return value
    }

    override fun removeTag(index: Int): LongTag = LongTag(this.remove(index))

    override fun copy(): LongArrayTag = LongArrayTag(this.elements.copyOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.elements.contentEquals((other as LongArrayTag).elements)
    }

    override fun hashCode(): Int = this.elements.contentHashCode()


    companion object Type : TagType<LongArrayTag> {
        override val typeId get() = TAG_LONG_ARRAY
        override fun toString() = "TAG_Long_Array"
        override fun read(input: DataInput, depth: Int) = LongArrayTag(
            LongArray(input.readInt()) { input.readLong() }
        )

        override fun transform(tag: BinaryTag) = when (tag) {
            is NumericTag -> LongArrayTag(longArrayOf(tag.toLong()))
            is StringTag -> LongArrayTag(longArrayOf(tag.value.toLongOrNull() ?: 0L))
            is ByteArrayTag -> LongArrayTag(LongArray(tag.size) { tag[it].toLong() })
            is IntArrayTag -> LongArrayTag(LongArray(tag.size) { tag[it].toLong() })
            is LongArrayTag -> tag
            is ListTag -> LongArrayTag(LongArray(tag.size) { tag[it].toLongTag().value })
            is CompoundTag -> {
                val elements = LongArray(tag.size)
                var index = 0
                tag.forEachSorted {
                    elements[index++] = it.toLongTag().value
                }
                LongArrayTag(elements)
            }

            else -> LongArrayTag()
        }
    }
}