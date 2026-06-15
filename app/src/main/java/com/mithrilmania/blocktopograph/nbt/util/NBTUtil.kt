package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.EMPTY_CHAR
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.nbt.TAG_END
import com.mithrilmania.blocktopograph.nbt.io.SNBTStringReader

val SIMPLE_VALUE: Regex = "[A-Za-z0-9._+-]+".toRegex()

fun String.parseSNBT() = try {
    SNBTParser(SNBTStringReader(this)).parseRoot()
} catch (_: Exception) {
    null
}

fun StringBuilder.appendQuoted(text: String): StringBuilder {
    var quote = EMPTY_CHAR
    val length = this.length
    this.append(' ')
    text.forEach { char ->
        if (char == '\\') {
            this.append('\\')
        } else if (char == '"' || char == '\'') {
            if (quote == EMPTY_CHAR) {
                quote = if (char == '"') '\'' else '"'
            }
            if (quote == char) {
                this.append('\\')
            }
        }
        this.append(char)
    }
    if (quote == EMPTY_CHAR) {
        quote = '"'
    }
    this.setCharAt(length, quote)
    return this.append(quote)
}

fun StringBuilder.appendSafeLiteral(
    text: String
): StringBuilder = if (SIMPLE_VALUE matches text) {
    this.append(text)
} else {
    this.appendQuoted(text)
}

inline fun <T> StringBuilder.append(
    iterable: Iterable<T>,
    indentation: Indentation,
    action: (T) -> Unit
) {
    val iterator = iterable.iterator()
    if (iterator.hasNext()) {
        indentation.beginStructure(this)
        action(iterator.next())
        while (iterator.hasNext()) {
            this.append(',')
            indentation.applyToElement(this)
            action(iterator.next())
        }
        indentation.endStructure(this)
    }
}

fun Char.isSafeLiteral() = when (this) {
    in '0'..'9',
    in 'A'..'Z',
    in 'a'..'z',
    '_', '-', '.', '+' -> true

    else -> false
}

fun BinaryTag.boxed(): CompoundTag {
    if (this is CompoundTag) return this
    val tags = hashMapOf<String, BinaryTag>()
    tags[""] = this
    return CompoundTag(tags)
}

fun CompoundTag.unbox(): BinaryTag? = if (this.size == 1) this[""] else null

fun BinaryTag.unboxOrSelf(): BinaryTag = if (this is CompoundTag) (this.unbox() ?: this) else this

fun BinaryTag?.extracted(): BinaryTag? = when (this) {
    is ByteArrayTag -> this.elements.firstOrNull()?.let(::ByteTag)
    is IntArrayTag -> this.elements.firstOrNull()?.let(::IntTag)
    is LongArrayTag -> this.elements.firstOrNull()?.let(::LongTag)
    is ListTag -> this.firstOrNull()
    is CompoundTag -> this.unbox()
    else -> this
}

fun BinaryTag?.toByteTag(): ByteTag = when (val tag = this.extracted()) {
    is NumericTag -> ByteTag(tag.toByte())
    is StringTag -> ByteTag(tag.value.toByteOrNull() ?: 0.toByte())
    else -> ByteTag(0.toByte())
}

fun BinaryTag?.toShortTag(): ShortTag = when (val tag = this.extracted()) {
    is NumericTag -> ShortTag(tag.toShort())
    is StringTag -> ShortTag(tag.value.toShortOrNull() ?: 0.toShort())
    else -> ShortTag(0.toShort())
}

fun BinaryTag?.toIntTag(): IntTag = when (val tag = this.extracted()) {
    is NumericTag -> IntTag(tag.toInt())
    is StringTag -> IntTag(tag.value.toIntOrNull() ?: 0)
    else -> IntTag(0)
}

fun BinaryTag?.toLongTag(): LongTag = when (val tag = this.extracted()) {
    is NumericTag -> LongTag(tag.toLong())
    is StringTag -> LongTag(tag.value.toLongOrNull() ?: 0L)
    else -> LongTag(0L)
}

fun BinaryTag?.toFloatTag(): FloatTag = when (val tag = this.extracted()) {
    is NumericTag -> FloatTag(tag.toFloat())
    is StringTag -> FloatTag(tag.value.toFloatOrNull() ?: 0.0F)
    else -> FloatTag(0.0F)

}

fun BinaryTag?.toDoubleTag(): DoubleTag = when (val tag = this.extracted()) {
    is NumericTag -> DoubleTag(tag.toDouble())
    is StringTag -> DoubleTag(tag.value.toDoubleOrNull() ?: 0.0)
    else -> DoubleTag(0.0)
}

inline fun <T> List<T>.getHomogenousTypeId(typeId: (T) -> Byte): Byte {
    var first = TAG_END
    this.forEach {
        val type = typeId(it)
        if (first != type) {
            if (first != TAG_END) return TAG_COMPOUND
            first = type
        }
    }
    return first
}
