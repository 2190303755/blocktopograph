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

val NbtTag?.stringValue: String? get() = (this as? NbtString)?.value
val NbtTag?.byteValue: Byte? get() = (this as? NbtByte)?.value
val NbtTag?.shortValue: Short? get() = (this as? NbtShort)?.value
val NbtTag?.intValue: Int? get() = (this as? NbtInt)?.value
val NbtTag?.longValue: Long? get() = (this as? NbtLong)?.value
val NbtTag?.doubleValue: Double? get() = (this as? NbtDouble)?.value
val NbtTag?.floatValue: Float? get() = (this as? NbtFloat)?.value

fun NbtCompound.wrapBeforeSave() =
    if (this.size == 1) this
    else NbtCompound(HashMap<String, NbtTag>().also { it[""] = this })

fun NbtCompound.unwrapAfterRead() =
    if (this.size == 1) this.values.firstOrNull() as? NbtCompound ?: this else this

fun String.asTag() = NbtString(this)

/*
fun NbtCompound.getGameMode(context: Context, bedrock: Boolean = false): String {
    return this.getInt(WORLD_GAME_MODE).let {
        when {
            it == 0 -> context.getString(R.string.activity_nbt_editor_game_mode_0)
            it == 1 -> context.getString(R.string.activity_nbt_editor_game_mode_1)
            it == 2 -> context.getString(R.string.activity_nbt_editor_game_mode_2)
            it == 3 && !bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_java_3)
            it == 3 && bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_bedrock_3)
            it == 4 && bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_bedrock_4)
            it == 5 && bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_bedrock_5)
            it == 6 && bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_bedrock_6)
            else -> context.getString(R.string.activity_nbt_editor_game_mode_unknown, it)
        }
    }
}*/

inline fun Map<String, NbtTag>.modifyAsCompound(action: MutableMap<String, NbtTag>.() -> Unit) =
    NbtCompound(this.toMutableMap().also(action))

fun detectHeader(data: ByteArray): Byte? =
    if (data.size > 7 &&
        data[3] == BYTE_0 &&
        data[1] == BYTE_0 &&
        data[2] == BYTE_0 &&
        (data[5].toInt() shl 8) + (data[4].toInt() and 255) + 8 == data.size
    ) data[0] else null

fun NbtTag.asResult(
    snbt: Boolean,
    version: Byte? = null,
    variant: NbtVariant = NbtVariant.Bedrock,
    compression: NbtCompression = NbtCompression.None
): ParsedNBT {
    if (this !is NbtCompound) return ParsedNBT(false, "", NbtCompound(emptyMap()))
    if (this.size == 1) {
        this.entries.firstOrNull()?.let { pair ->
            val data = pair.value as? NbtCompound
            if (data != null) {
                return ParsedNBT(snbt, pair.key, data, version, variant, compression)
            }
        }
    }
    return ParsedNBT(snbt, "", this, version, variant, compression)
}

// TODO: use okio or stream instead of byte array
fun InputStream.readUnknownNBT(): ParsedNBT {
    var data = this.readBytes()
    try {
        var version: Byte? = null
        detectHeader(data)?.let { value ->
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
    return ParsedNBT(false, "", NbtCompound(emptyMap()))
}

fun InputStream.readCompound(
    compression: NbtCompression = NbtCompression.Gzip,
    variant: NbtVariant = NbtVariant.Bedrock
) = Nbt {
    this.variant = variant
    this.compression = compression
}.decodeFromStream<NbtCompound>(this)