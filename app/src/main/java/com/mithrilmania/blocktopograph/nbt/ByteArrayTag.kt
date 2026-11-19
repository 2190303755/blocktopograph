package com.mithrilmania.blocktopograph.nbt

import androidx.compose.ui.util.fastForEach
import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.toByteTag
import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.bytes.ByteList
import java.io.DataInput
import java.io.DataOutput

fun mutableByteListOf(value: Byte): ByteArrayList {
    val elements = ByteArrayList()
    elements.add(value)
    return elements
}

inline fun <T> List<T>.toByteList(transform: (T) -> Byte): ByteArrayList {
    val elements = ByteArrayList(this.size)
    this.fastForEach {
        elements.add(transform(it))
    }
    return elements
}

inline fun ByteList.forEachByte(action: (Byte) -> Unit) {
    for (index in this.indices) {
        action(this.getByte(index))
    }
}

@JvmInline
value class ByteArrayTag(
    val elements: ByteArrayList = ByteArrayList()
) : ArrayTag<ByteTag> {
    override val elementTypeId: Byte get() = TAG_BYTE
    override val type: Type get() = Type
    override val size: Int get() = this.elements.size
    override fun isEmpty(): Boolean = this.elements.isEmpty
    operator fun get(index: Int): Byte = this.elements.getByte(index)
    override fun getAsLong(index: Int): Long = this[index].toLong()
    override fun getAsTag(index: Int): ByteTag = ByteTag(this[index])
    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    override fun write(output: DataOutput) {
        val size = this.size
        output.writeInt(size)
        output.write(this.elements.elements(), 0, size)
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
            this.elements.add(index, tag.toByte())
            return true
        }
        return false
    }

    override fun clear() {
        this.elements.clear()
    }

    fun removeAt(index: Int): Byte {
        return this.elements.removeByte(index)
    }

    override fun removeTag(index: Int): ByteTag = ByteTag(this.removeAt(index))

    override fun copy(): ByteArrayTag = ByteArrayTag(ByteArrayList(this.elements))

    companion object Type : TagType<ByteArrayTag> {
        override val typeId get() = TAG_BYTE_ARRAY
        override fun toString() = "TAG_Byte_Array"
        override fun read(input: DataInput, depth: Int) = ByteArrayTag(
            ByteArrayList.wrap(
                ByteArray(input.readInt()).also { input.readFully(it) }
            )
        )

        override fun transform(tag: BinaryTag) = when (tag) {
            is NumericTag -> ByteArrayTag(mutableByteListOf(tag.toByte()))
            is StringTag -> ByteArrayTag(
                mutableByteListOf(
                    tag.value.trim().toByteOrNull() ?: 0.toByte()
                )
            )

            is ListTag -> ByteArrayTag(tag.tags.toByteList { it.toByteTag().value })
            is ByteArrayTag -> tag
            is IntArrayTag -> {
                val elements = ByteArrayList(tag.size)
                tag.elements.forEach {
                    elements.add(it.toByte())
                }
                ByteArrayTag(elements)
            }

            is LongArrayTag -> {
                val elements = ByteArrayList(tag.size)
                tag.elements.forEach {
                    elements.add(it.toByte())
                }
                ByteArrayTag(elements)
            }

            is CompoundTag -> {
                val elements = ByteArrayList(tag.size)
                tag.forEachSorted {
                    elements.add(it.toByteTag().value)
                }
                ByteArrayTag(elements)
            }

            else -> ByteArrayTag()
        }
    }
}