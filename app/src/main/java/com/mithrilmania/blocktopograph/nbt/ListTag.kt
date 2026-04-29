package com.mithrilmania.blocktopograph.nbt

import android.os.Build
import androidx.annotation.RequiresApi
import com.mithrilmania.blocktopograph.nbt.io.increaseDepthOrThrow
import com.mithrilmania.blocktopograph.nbt.util.NBTFormatException
import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.boxed
import com.mithrilmania.blocktopograph.nbt.util.getHomogenousTypeId
import com.mithrilmania.blocktopograph.nbt.util.unboxOrSelf
import java.io.DataInput
import java.io.DataOutput
import java.util.function.IntFunction

@JvmInline
value class ListTag(
    @JvmField val tags: MutableList<BinaryTag> = mutableListOf(),
) : CollectionTag<BinaryTag>, MutableList<BinaryTag> by tags {
    override val type: Type get() = Type
    override fun getAsTag(index: Int): BinaryTag = this.tags[index]
    override val elementTypeId: Byte
        get() = this.getHomogenousTypeId { it.type.typeId }

    override fun setTag(index: Int, tag: BinaryTag): Boolean {
        this.tags[index] = tag
        return true
    }

    override fun addTag(index: Int, tag: BinaryTag): Boolean {
        this.tags.add(index, tag)
        return true
    }

    override fun removeTag(index: Int): BinaryTag = this.tags.removeAt(index)

    override fun copy(): ListTag {
        val tags = mutableListOf<BinaryTag>()
        this.tags.forEach {
            tags.add(it.copy())
        }
        return ListTag(tags)
    }

    override fun write(output: DataOutput) {
        val typeId = this.elementTypeId
        output.writeByte(typeId.toInt())
        output.writeInt(this.size)
        if (typeId == TAG_COMPOUND) {
            this.forEach {
                it.boxed().write(output)
            }
        } else {
            this.forEach {
                it.write(output)
            }
        }
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    companion object Type : TagType<ListTag> {
        override val typeId get() = TAG_LIST
        override fun toString() = "TAG_List"
        override fun read(input: DataInput, depth: Int): ListTag {
            val child = depth.increaseDepthOrThrow()
            val id = input.readByte()
            val length = input.readInt()
            if (id == TAG_END && length > 0) throw NBTFormatException("Missing type on ListTag")
            val type = id.toTagType()
            val tags = ArrayList<BinaryTag>(length)
            repeat(length) {
                tags.add(type.read(input, child).unboxOrSelf())
            }
            return ListTag(tags)
        }

        override fun transform(tag: BinaryTag) = when (tag) {
            EndTag -> ListTag()
            is ByteArrayTag -> ListTag(tag.elements.mapTo(mutableListOf(), ::ByteTag))
            is IntArrayTag -> ListTag(tag.elements.mapTo(mutableListOf(), ::IntTag))
            is LongArrayTag -> ListTag(tag.elements.mapTo(mutableListOf(), ::LongTag))
            is ListTag -> tag
            is CompoundTag -> {
                val tags = mutableListOf<BinaryTag>()
                tag.forEachSorted(tags::add)
                ListTag(tags)
            }

            else -> ListTag(mutableListOf(tag))
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Deprecated(
        "This declaration is redundant in Kotlin",
        replaceWith = ReplaceWith("toTypedArray")
    )
    override fun <T> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?> =
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        (this.tags as java.util.Collection<*>).toArray(generator)
}