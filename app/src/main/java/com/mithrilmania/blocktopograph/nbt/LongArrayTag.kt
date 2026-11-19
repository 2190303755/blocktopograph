package com.mithrilmania.blocktopograph.nbt

import androidx.collection.MutableLongList
import androidx.collection.mutableLongListOf
import androidx.compose.ui.util.fastForEach
import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toLongTag
import java.io.DataInput
import java.io.DataOutput

inline fun <T> List<T>.toLongList(transform: (T) -> Long): MutableLongList {
    val elements = MutableLongList(this.size)
    this.fastForEach {
        elements.add(transform(it))
    }
    return elements
}

@JvmInline
value class LongArrayTag(
    val elements: MutableLongList = mutableLongListOf()
) : ArrayTag<LongTag> {
    override val elementTypeId: Byte get() = TAG_LONG
    override val type: Type get() = Type
    override val size: Int get() = this.elements.size
    override fun isEmpty(): Boolean = this.elements.isEmpty()
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
            this.elements.add(index, tag.toLong())
            return true
        }
        return false
    }

    override fun clear() {
        this.elements.clear()
    }

    fun removeAt(index: Int): Long {
        return this.elements.removeAt(index)
    }

    override fun removeTag(index: Int): LongTag = LongTag(this.removeAt(index))

    override fun copy(): LongArrayTag {
        val copy = MutableLongList(this.elements.size)
        copy.addAll(this.elements)
        return LongArrayTag(copy)
    }

    companion object Type : TagType<LongArrayTag> {
        override val typeId get() = TAG_LONG_ARRAY
        override fun toString() = "TAG_Long_Array"
        override fun read(input: DataInput, depth: Int): LongArrayTag {
            val length = input.readInt()
            val elements = MutableLongList(length)
            repeat(length) {
                elements.add(input.readLong())
            }
            return LongArrayTag(elements)
        }

        override fun transform(tag: BinaryTag) = when (tag) {
            is NumericTag -> LongArrayTag(mutableLongListOf(tag.toLong()))
            is StringTag -> LongArrayTag(mutableLongListOf(tag.value.trim().toLongOrNull() ?: 0L))
            is ListTag -> LongArrayTag(tag.toLongList { it.toLongTag().value })
            is LongArrayTag -> tag
            is ByteArrayTag -> {
                val elements = MutableLongList(tag.size)
                tag.elements.forEachByte {
                    elements.add(it.toLong())
                }
                LongArrayTag(elements)
            }

            is IntArrayTag -> {
                val elements = MutableLongList(tag.size)
                tag.elements.forEach {
                    elements.add(it.toLong())
                }
                LongArrayTag(elements)
            }
            is CompoundTag -> {
                val elements = MutableLongList(tag.size)
                tag.forEachSorted {
                    elements.add(it.toLongTag().value)
                }
                LongArrayTag(elements)
            }

            else -> LongArrayTag()
        }
    }
}