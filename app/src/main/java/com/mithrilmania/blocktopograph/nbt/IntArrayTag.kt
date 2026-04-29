package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toIntTag
import com.mithrilmania.blocktopograph.util.expandAt
import com.mithrilmania.blocktopograph.util.shrinkAt
import java.io.DataInput
import java.io.DataOutput

data class IntArrayTag(
    @JvmField
    var elements: IntArray = IntArray(0)
) : ArrayTag<IntTag> {
    override val elementTypeId: Byte get() = TAG_INT
    override val type: Type get() = Type
    override val size: Int get() = this.elements.size
    operator fun get(index: Int): Int = this.elements[index]
    override fun getAsLong(index: Int): Long = this[index].toLong()
    override fun getAsTag(index: Int): IntTag = IntTag(this[index])
    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    override fun write(output: DataOutput) {
        output.writeInt(this.size)
        this.elements.forEach(output::writeInt)
    }

    operator fun set(index: Int, value: Int) {
        this.elements[index] = value
    }

    override fun setTag(index: Int, tag: BinaryTag): Boolean {
        if (tag is NumericTag) {
            this[index] = tag.toInt()
            return true
        }
        return false
    }

    override fun addTag(index: Int, tag: BinaryTag): Boolean {
        if (tag is NumericTag) {
            this.elements = this.elements.expandAt(index, ::IntArray)
            this.elements[index] = tag.toInt()
            return true
        }
        return false
    }

    override fun clear() {
        this.elements = IntArray(0)
    }

    fun remove(index: Int): Int {
        val value = this.elements[index]
        this.elements = this.elements.shrinkAt(index, ::IntArray)
        return value
    }

    override fun removeTag(index: Int): IntTag = IntTag(this.remove(index))

    override fun copy(): IntArrayTag = IntArrayTag(this.elements.copyOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.elements.contentEquals((other as IntArrayTag).elements)
    }

    override fun hashCode(): Int = this.elements.contentHashCode()


    companion object Type : TagType<IntArrayTag> {
        override val typeId get() = TAG_INT_ARRAY
        override fun toString() = "TAG_Int_Array"
        override fun read(input: DataInput, depth: Int) = IntArrayTag(
            IntArray(input.readInt()) { input.readInt() }
        )

        override fun transform(tag: BinaryTag) = when (tag) {
            is NumericTag -> IntArrayTag(intArrayOf(tag.toInt()))
            is StringTag -> IntArrayTag(intArrayOf(tag.value.toIntOrNull() ?: 0))
            is ByteArrayTag -> IntArrayTag(IntArray(tag.size) { tag[it].toInt() })
            is IntArrayTag -> tag
            is LongArrayTag -> IntArrayTag(IntArray(tag.size) { tag[it].toInt() })
            is ListTag -> IntArrayTag(IntArray(tag.size) { tag[it].toIntTag().value })
            is CompoundTag -> {
                val elements = IntArray(tag.size)
                var index = 0
                tag.forEachSorted {
                    elements[index++] = it.toIntTag().value
                }
                IntArrayTag(elements)
            }

            else -> IntArrayTag()
        }
    }
}