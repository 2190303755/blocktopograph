package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.io.TagReader
import com.mithrilmania.blocktopograph.nbt.util.NBTFormatException
import java.io.DataInput

const val TAG_END: Byte = 0x00
const val TAG_BYTE: Byte = 0x01
const val TAG_SHORT: Byte = 0x02
const val TAG_INT: Byte = 0x03
const val TAG_LONG: Byte = 0x04
const val TAG_FLOAT: Byte = 0x05
const val TAG_DOUBLE: Byte = 0x06
const val TAG_BYTE_ARRAY: Byte = 0x07
const val TAG_STRING: Byte = 0x08
const val TAG_LIST: Byte = 0x09
const val TAG_COMPOUND: Byte = 0x0A
const val TAG_INT_ARRAY: Byte = 0x0B
const val TAG_LONG_ARRAY: Byte = 0x0C
const val KINDS_OF_SELECTABLE_TAGS: Int = TAG_LONG_ARRAY.toInt()

sealed interface TagType<T : BinaryTag> : TagReader<T> {
    val typeId: Byte
    fun transform(tag: BinaryTag): T
    override fun toString(): String
}

sealed interface NumericTagType<T : NumericTag> : TagType<T>

fun Byte.toTagType(): TagType<*> = when (this) {
    TAG_END -> EndTag
    TAG_BYTE -> ByteTag.Type
    TAG_SHORT -> ShortTag.Type
    TAG_INT -> IntTag.Type
    TAG_LONG -> LongTag.Type
    TAG_FLOAT -> FloatTag.Type
    TAG_DOUBLE -> DoubleTag.Type
    TAG_BYTE_ARRAY -> ByteArrayTag.Type
    TAG_STRING -> StringTag.Type
    TAG_LIST -> ListTag.Type
    TAG_COMPOUND -> CompoundTag.Type
    TAG_INT_ARRAY -> IntArrayTag.Type
    TAG_LONG_ARRAY -> LongArrayTag.Type
    else -> Invalid(this)
}

@JvmInline
private value class Invalid(override val typeId: Byte) : TagType<EndTag> {
    override fun toString() = "UNKNOWN_$typeId"
    override fun transform(tag: BinaryTag) = fail()
    override fun read(input: DataInput, depth: Int) = fail()
    fun fail(): Nothing = throw NBTFormatException("Invalid tag type: $typeId")
}