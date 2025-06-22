package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.util.BYTE_0
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtDouble
import net.benwoodworth.knbt.NbtFloat
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtLong
import net.benwoodworth.knbt.NbtShort
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.NbtVariant
import net.benwoodworth.knbt.StringifiedNbt
import net.benwoodworth.knbt.decodeFromStream
import net.benwoodworth.knbt.detect
import java.io.InputStream
import java.nio.charset.Charset

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

val EMPTY_COMPOUND = NbtCompound(emptyMap())

val NbtTag?.stringValue: String? get() = (this as? NbtString)?.value
val NbtTag?.byteValue: Byte? get() = (this as? NbtByte)?.value
val NbtTag?.shortValue: Short? get() = (this as? NbtShort)?.value
val NbtTag?.intValue: Int? get() = (this as? NbtInt)?.value
val NbtTag?.longValue: Long? get() = (this as? NbtLong)?.value
val NbtTag?.doubleValue: Double? get() = (this as? NbtDouble)?.value
val NbtTag?.floatValue: Float? get() = (this as? NbtFloat)?.value

fun NbtCompound.wrap(name: String = "") =
    if (name.isEmpty() && this.size == 1) this
    else NbtCompound(HashMap<String, NbtTag>().also { it[name] = this })

fun NbtCompound.unwrap() =
    if (this.size == 1) this.values.firstOrNull() as? NbtCompound ?: this else this

fun String.asTag() = NbtString(this)

inline fun Map<String, NbtTag>.modifyAsCompound(action: MutableMap<String, NbtTag>.() -> Unit) =
    NbtCompound(this.toMutableMap().also(action))

fun ByteArray.readHeader(): Byte? =
    if (this.size > 7 &&
        this[3] == BYTE_0 &&
        this[1] == BYTE_0 &&
        this[2] == BYTE_0 &&
        (this[5].toInt() shl 8) + (this[4].toInt() and 255) + 8 == this.size
    ) this[0] else null

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

// TODO: use okio or stream instead of byte array
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

fun InputStream.readCompound(
    compression: NbtCompression = NbtCompression.Gzip,
    variant: NbtVariant = NbtVariant.Bedrock
) = Nbt {
    this.variant = variant
    this.compression = compression
}.decodeFromStream<NbtCompound>(this)

fun Char.isSafe(): Boolean = when (this) {
    '-', '_', in 'a'..'z', in 'A'..'Z', in '0'..'9' -> true
    else -> false
}

// TODO check preference of Mojang
fun Appendable.appendSafeLiteral(value: String): Appendable = when {
    value.isEmpty() -> append("''")
    value.all(Char::isSafe) -> append(value)
    !value.contains('\'') -> append('\'').append(value).append('\'')
    !value.contains('"') -> append('"').append(value).append('"')
    else -> {
        append('"')
        value.forEach {
            if (it == '"') {
                append("\\\"")
            } else {
                append(it)
            }
        }
        append('"')
    }
}