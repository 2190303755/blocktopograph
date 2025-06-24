package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.EMPTY_CHAR
import com.mithrilmania.blocktopograph.nbt.io.NBTInput
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser.Companion.DOUBLE_QUOTE
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser.Companion.SINGLE_QUOTE
import com.mithrilmania.blocktopograph.util.BYTE_0
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.NbtVariant
import net.benwoodworth.knbt.StringifiedNbt
import net.benwoodworth.knbt.decodeFromStream
import net.benwoodworth.knbt.detect
import java.io.InputStream
import java.nio.charset.Charset
import java.util.regex.Pattern

val SIMPLE_VALUE: Pattern = Pattern.compile("[A-Za-z0-9._+-]+")

@Deprecated("")
val EMPTY_COMPOUND = NbtCompound(emptyMap())

@Deprecated("")
fun NbtCompound.wrap(name: String = "") =
    if (name.isEmpty() && this.size == 1) this
    else NbtCompound(HashMap<String, NbtTag>().also { it[name] = this })

@Deprecated("")
fun NbtCompound.unwrap() =
    if (this.size == 1) this.values.firstOrNull() as? NbtCompound ?: this else this

@Deprecated("")
fun String.asTag() = NbtString(this)

@Deprecated("")
inline fun Map<String, NbtTag>.modifyAsCompound(action: MutableMap<String, NbtTag>.() -> Unit) =
    NbtCompound(this.toMutableMap().also(action))

@Deprecated("")
fun ByteArray.readHeader(): Byte? =
    if (this.size > 7 &&
        this[3] == BYTE_0 &&
        this[1] == BYTE_0 &&
        this[2] == BYTE_0 &&
        (this[5].toInt() shl 8) + (this[4].toInt() and 255) + 8 == this.size
    ) this[0] else null

@Deprecated("")
fun NbtTag.asResult(
    snbt: Boolean,
    version: Byte? = null,
    variant: NbtVariant = NbtVariant.Bedrock,
    compression: NbtCompression = NbtCompression.None
): NBTInput {
    if (this !is NbtCompound) return NBTInput(false, "", EMPTY_COMPOUND)
    if (this.size == 1) {
        this.entries.firstOrNull()?.let { pair ->
            val data = pair.value as? NbtCompound
            if (data != null) {
                return NBTInput(snbt, pair.key, data, version, variant, compression)
            }
        }
    }
    return NBTInput(snbt, "", this, version, variant, compression)
}

@Deprecated("")
fun InputStream.readUnknownNBT(): NBTInput {
    var data = this.readBytes()
    try {
        var version: Byte? = null
        data.readHeader()?.let { value ->
            version = value
            data = data.copyOfRange(8, data.size)
        }
        val compression = NbtCompression.Companion.detect(data)
        for (variant in arrayOf(
            NbtVariant.Bedrock,
            NbtVariant.Java,
            NbtVariant.BedrockNetwork
        )) {
            try {
                return Nbt {
                    this.variant = variant
                    this.compression = compression
                }.decodeFromByteArray(NbtTag.Companion.serializer(), data)
                    .asResult(false, version, variant, compression)
            } catch (_: Exception) {
            }
        }
    } catch (_: Exception) {
        try {
            return StringifiedNbt { }.decodeFromString(
                NbtTag.Companion.serializer(),
                data.toString(Charset.defaultCharset())
            ).asResult(true)
        } catch (_: Exception) {
        }
    }
    return NBTInput(false, "", EMPTY_COMPOUND)
}

@Deprecated("")
fun InputStream.readCompound(
    compression: NbtCompression = NbtCompression.Gzip,
    variant: NbtVariant = NbtVariant.Bedrock
) = Nbt {
    this.variant = variant
    this.compression = compression
}.decodeFromStream<NbtCompound>(this)

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
): StringBuilder = if (SIMPLE_VALUE.matcher(text).matches()) {
    this.append(text)
} else {
    this.appendQuoted(text)
}

fun StringBuilder.indent(unit: String, depth: Int): StringBuilder {
    repeat(depth) { this.append(unit) }
    return this
}

inline fun <reified T> StringBuilder.append(
    iterable: Iterable<T>,
    indent: StringBuilder.() -> Unit,
    action: (T) -> Unit
) {
    val iterator = iterable.iterator()
    if (iterator.hasNext()) {
        this.indent()
        action(iterator.next())
        while (iterator.hasNext()) {
            this.append(',')
            this.indent()
            action(iterator.next())
        }
    }
}

fun Char.isQuote() = when (this) {
    DOUBLE_QUOTE, SINGLE_QUOTE -> true
    else -> false
}

fun Char.isSafeLiteral() = when (this) {
    in '0'..'9',
    in 'A'..'Z',
    in 'a'..'z',
    '_', '-', '.', '+' -> true

    else -> false
}