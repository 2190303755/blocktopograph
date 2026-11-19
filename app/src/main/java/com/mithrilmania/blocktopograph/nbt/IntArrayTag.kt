package com.mithrilmania.blocktopograph.nbt

import androidx.collection.MutableIntList
import androidx.collection.mutableIntListOf
import androidx.compose.ui.util.fastForEach
import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toIntTag
import java.io.DataInput
import java.io.DataOutput

inline fun <T> List<T>.toIntList(transform: (T) -> Int): MutableIntList {
    val elements = MutableIntList(this.size)
    this.fastForEach {
        elements.add(transform(it))
    }
    return elements
}

@JvmInline
value class IntArrayTag(
    val elements: MutableIntList = mutableIntListOf()
) : ArrayTag<IntTag> {
    override val elementTypeId: Byte get() = TAG_INT
    override val type: Type get() = Type
    override val size: Int get() = this.elements.size
    override fun isEmpty(): Boolean = this.elements.isEmpty()
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
            this.elements.add(index, tag.toInt())
            return true
        }
        return false
    }

    override fun clear() {
        this.elements.clear()
    }

    fun removeAt(index: Int): Int {
        return this.elements.removeAt(index)
    }

    override fun removeTag(index: Int): IntTag = IntTag(this.removeAt(index))

    override fun copy(): IntArrayTag {
        val copy = MutableIntList(this.elements.size)
        copy.addAll(this.elements)
        return IntArrayTag(copy)
    }

    companion object Type : TagType<IntArrayTag> {
        override val typeId get() = TAG_INT_ARRAY
        override fun toString() = "TAG_Int_Array"
        override fun read(input: DataInput, depth: Int): IntArrayTag {
            input.readUTF()
            val length = input.readInt()
            val elements = MutableIntList(length)
            repeat(length) {
                elements.add(input.readInt())
            }
            return IntArrayTag(elements)
        }

        override fun transform(tag: BinaryTag) = when (tag) {
            is NumericTag -> IntArrayTag(mutableIntListOf(tag.toInt()))
            is StringTag -> IntArrayTag(mutableIntListOf(tag.value.trim().toIntOrNull() ?: 0))
            is ListTag -> IntArrayTag(tag.toIntList { it.toIntTag().value })
            is IntArrayTag -> tag
            is ByteArrayTag -> {
                val elements = MutableIntList(tag.size)
                tag.elements.forEachByte {
                    elements.add(it.toInt())
                }
                IntArrayTag(elements)
            }

            is LongArrayTag -> {
                val elements = MutableIntList(tag.size)
                tag.elements.forEach {
                    elements.add(it.toInt())
                }
                IntArrayTag(elements)
            }
            is CompoundTag -> {
                val elements = MutableIntList(tag.size)
                tag.forEachSorted {
                    elements.add(it.toIntTag().value)
                }
                IntArrayTag(elements)
            }

            else -> IntArrayTag()
        }
    }
}