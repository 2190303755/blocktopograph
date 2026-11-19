package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.EMPTY_CHAR
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CollectionTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.nbt.TAG_END
import com.mithrilmania.blocktopograph.util.math.Vec3f
import com.mithrilmania.blocktopograph.util.tryOrNull

@JvmField
val SIMPLE_VALUE: Regex = "[A-Za-z0-9._+-]+".toRegex()

fun String.parseSNBT() = tryOrNull {
    SNBTParser(this.reader()).parseRoot()
}

fun String.hexToCharCode(): Int {
    val code = this.hexToInt()
    if (Character.isValidCodePoint(code)) return code
    throw IllegalArgumentException("Invalid code point $this")
}

fun StringBuilder.appendQuoted(text: String): StringBuilder {
    var quote = EMPTY_CHAR
    val leading = this.length
    this.append(DOUBLE_QUOTE_CHAR)
    text.forEach { char ->
        if (char == BACKSLASH_CHAR) {
            this.append(BACKSLASH_CHAR)
        } else if (char == DOUBLE_QUOTE_CHAR || char == SINGLE_QUOTE_CHAR) {
            if (quote == EMPTY_CHAR) {
                quote = if (char == DOUBLE_QUOTE_CHAR) SINGLE_QUOTE_CHAR else DOUBLE_QUOTE_CHAR
            } else if (quote == char) {
                this.append(BACKSLASH_CHAR)
            }
        }
        this.append(char)
    }
    if (quote == SINGLE_QUOTE_CHAR) {
        this.setCharAt(leading, SINGLE_QUOTE_CHAR)
        return this.append(SINGLE_QUOTE_CHAR)
    }
    return this.append(DOUBLE_QUOTE_CHAR)
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

fun Char.isValidInLiteral() = when (this) {
    in '0'..'9',
    in 'A'..'Z',
    in 'a'..'z',
    '_', '-', '.', '+' -> true

    else -> false
}

fun BinaryTag.ensureCompound(): CompoundTag {
    if (this is CompoundTag) return this
    val tags = hashMapOf<String, BinaryTag>()
    tags[""] = this
    return CompoundTag(tags)
}

fun CompoundTag.unwrap(): BinaryTag? = if (this.size == 1) this[""] else null

fun BinaryTag.unwrapOrSelf(): BinaryTag = if (this is CompoundTag) (this.unwrap() ?: this) else this

fun BinaryTag?.extracted(): BinaryTag? = when (this) {
    is CompoundTag -> this.unwrap()
    is CollectionTag<*> -> if (this.isEmpty()) null else this.getAsTag(0)
    else -> this
}

fun BinaryTag?.toByteTag(): ByteTag = when (val tag = this.extracted()) {
    is NumericTag -> ByteTag(tag.toByte())
    is StringTag -> ByteTag(tag.value.trim().toByteOrNull() ?: 0.toByte())
    else -> ByteTag(0.toByte())
}

fun BinaryTag?.toShortTag(): ShortTag = when (val tag = this.extracted()) {
    is NumericTag -> ShortTag(tag.toShort())
    is StringTag -> ShortTag(tag.value.trim().toShortOrNull() ?: 0.toShort())
    else -> ShortTag(0.toShort())
}

fun BinaryTag?.toIntTag(): IntTag = when (val tag = this.extracted()) {
    is NumericTag -> IntTag(tag.toInt())
    is StringTag -> IntTag(tag.value.trim().toIntOrNull() ?: 0)
    else -> IntTag(0)
}

fun BinaryTag?.toLongTag(): LongTag = when (val tag = this.extracted()) {
    is NumericTag -> LongTag(tag.toLong())
    is StringTag -> LongTag(tag.value.trim().toLongOrNull() ?: 0L)
    else -> LongTag(0L)
}

fun BinaryTag?.toFloatTag(): FloatTag = when (val tag = this.extracted()) {
    is NumericTag -> FloatTag(tag.toFloat())
    is StringTag -> FloatTag(tag.value.trim().toFloatOrNull() ?: 0.0F)
    else -> FloatTag(0.0F)

}

fun BinaryTag?.toDoubleTag(): DoubleTag = when (val tag = this.extracted()) {
    is NumericTag -> DoubleTag(tag.toDouble())
    is StringTag -> DoubleTag(tag.value.trim().toDoubleOrNull() ?: 0.0)
    else -> DoubleTag(0.0)
}

fun CollectionTag<*>.resolveVec3f(): Vec3f? = if (this.size == 3) Vec3f(
    (this.getAsTag(0) as? NumericTag ?: return null).toFloat(),
    (this.getAsTag(1) as? NumericTag ?: return null).toFloat(),
    (this.getAsTag(2) as? NumericTag ?: return null).toFloat()
) else null

inline fun <T> List<T>.getHomogenousTypeId(typeId: (T) -> Byte): Byte {
    var homogenous = TAG_END
    this.forEach {
        val current = typeId(it)
        if (homogenous != current) {
            if (homogenous != TAG_END) return TAG_COMPOUND
            homogenous = current
        }
    }
    return homogenous
}
