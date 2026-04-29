package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput

sealed interface BinaryTag {
    val type: TagType<*>
    fun copy(): BinaryTag
    fun write(output: DataOutput)
    fun accept(visitor: TagVisitor)
}

sealed interface PrimitiveTag : BinaryTag {
    override fun copy(): PrimitiveTag = this
    override fun toString(): String
}

sealed interface NumericTag : PrimitiveTag {
    override val type: NumericTagType<*>
    fun toNumber(): Number
    fun toByte(): Byte
    fun toShort(): Short
    fun toInt(): Int
    fun toLong(): Long
    fun toFloat(): Float
    fun toDouble(): Double
}

sealed interface CollectionTag<T : BinaryTag> : BinaryTag {
    val elementTypeId: Byte
    val size: Int
    fun clear()
    fun getAsTag(index: Int): T
    fun setTag(index: Int, tag: BinaryTag): Boolean
    fun addTag(index: Int, tag: BinaryTag): Boolean
    fun removeTag(index: Int): T
}

sealed interface ArrayTag<T : NumericTag> : CollectionTag<NumericTag> {
    fun getAsLong(index: Int): Long
}

object EndTag : BinaryTag, TagType<EndTag> {
    override val typeId: Byte get() = TAG_END
    override val type: TagType<*> get() = this
    override fun toString() = "TAG_End"
    override fun copy(): EndTag = this
    override fun transform(tag: BinaryTag): EndTag = EndTag
    override fun read(
        input: DataInput,
        depth: Int
    ): EndTag = EndTag

    override fun write(output: DataOutput) {}
    override fun accept(visitor: TagVisitor) {}
}
