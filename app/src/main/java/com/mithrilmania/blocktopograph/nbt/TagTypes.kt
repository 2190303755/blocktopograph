package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.io.TagReader
import com.mithrilmania.blocktopograph.nbt.util.NBTFormatException
import java.io.DataInput

const val TAG_END = 0
const val TAG_BYTE = 1
const val TAG_SHORT = 2
const val TAG_INT = 3
const val TAG_LONG = 4
const val TAG_FLOAT = 5
const val TAG_DOUBLE = 6
const val TAG_BYTE_ARRAY = 7
const val TAG_STRING = 8
const val TAG_LIST = 9
const val TAG_COMPOUND = 10
const val TAG_INT_ARRAY = 11
const val TAG_LONG_ARRAY = 12
const val TAG_ROOT = 13 // for layout

fun Int.asTagType(): TagType<*> = when (this) {
    TAG_END -> EndTagType
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

fun Int.isImmutableTagType(): Boolean = when (this) {
    TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE, TAG_STRING -> true
    else -> false
}

sealed interface TagType<T : BinaryTag<*>> : TagReader<T> {
    val id: Int
    override fun toString(): String
}

object EndTagType : TagType<EndTag> {
    override val id get() = TAG_END
    override fun toString() = "TAG_End"
    override fun read(input: DataInput, depth: Int) = EndTag
}

@JvmInline
private value class Invalid(override val id: Int) : TagType<EndTag> {
    override fun toString() = "UNKNOWN_$id"
    override fun read(input: DataInput, depth: Int) =
        throw NBTFormatException("Invalid tag type: $id")
}
