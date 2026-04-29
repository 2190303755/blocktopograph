package com.mithrilmania.blocktopograph.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.block.Block
import com.mithrilmania.blocktopograph.nbt.old.tags.ByteTag
import com.mithrilmania.blocktopograph.nbt.old.tags.CompoundTag
import com.mithrilmania.blocktopograph.nbt.old.tags.IntTag
import com.mithrilmania.blocktopograph.nbt.old.tags.ListTag
import com.mithrilmania.blocktopograph.nbt.old.tags.StringTag
import com.mithrilmania.blocktopograph.nbt.old.tags.Tag
import com.mithrilmania.blocktopograph.world.KEY_GAME_MODE
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_VERSION
import java.io.InputStream
import java.io.PushbackInputStream
import java.io.Serializable
import java.util.zip.GZIPInputStream

inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T {
    try {
        return enumValueOf<T>(this ?: return fallback)
    } catch (_: Exception) {
    }
    return fallback
}

fun InputStream.autoDecompress(
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): InputStream {
    val stream = PushbackInputStream(this, 2)
    val header = ByteArray(2)
    val read = stream.read(header)
    if (read > 0) {
        stream.unread(header, 0, read)
        if (read == 2 && GZIPInputStream.GZIP_MAGIC ==
            (header[0].toInt() and 0xFF) or ((header[1].toInt() and 0xFF) shl 8)
        ) {
            return GZIPInputStream(stream, bufferSize)
        }
    }
    return stream.buffered(bufferSize)
}

fun String.toChar(): Char = if (this.length == 1) this[0] else 0.toChar()

fun String.lenientHexToByteArray(): ByteArray {
    val text = this.trim()
    if (this.length > 1 && text[0] == '0' && (text[1] == 'x' || text[1] == 'X')) {
        return text.substring(2).hexToByteArray()
    }
    return text.hexToByteArray()
}

fun ClipData.collectText(): String? {
    val list = mutableListOf<CharSequence>()
    for (i in 0 until this.itemCount) {
        list.add(this.getItemAt(i).text ?: continue)
    }
    return if (list.isEmpty()) null else list.joinToString("\n")
}

fun CompoundTag?.getGameMode(context: Context) = (this?.getChildTagByKey(
    KEY_GAME_MODE
) as? IntTag)?.value.let {
    when (it) {
        0 -> context.getString(R.string.game_mode_survival)
        1 -> context.getString(R.string.game_mode_creative)
        2 -> context.getString(R.string.game_mode_adventure)
        6 -> context.getString(R.string.game_mode_spectator)
        else -> context.getString(R.string.game_mode_unknown, it.toString())
    }
}

val CompoundTag?.lastPlayedVersion: String
    get() {
        val list = (this?.getChildTagByKey(
            KEY_LAST_PLAYED_VERSION
        ) as? ListTag)?.value ?: return "Unknown"
        val iterator = list.iterator()
        if (!iterator.hasNext()) return "Unknown"
        val builder = StringBuilder(iterator.next().value.toString())
        while (iterator.hasNext()) {
            builder.append('.').append(iterator.next().value)
        }
        return builder.toString()
    }

private fun Any?.wrap(key: String): Tag<*> = when (this) {
    is Byte -> ByteTag(key, this)
    is Int -> IntTag(key, this)
    is String -> StringTag(key, this)
    else -> throw RuntimeException("block state with unsupported type")
}

fun Block.serializeState(): ArrayList<Tag<*>> {
    val props = this.type.knownProperties
    val values = this.knownProperties
    val custom = this.customProperties
    val size = minOf(props.size, values.size)
    val list = ArrayList<Tag<*>>(size + custom.size)
    for (i in 0 until size) {
        list += values[i].wrap(props[i].name)
    }
    custom.forEach { (key, value) ->
        list += value.wrap(key)
    }
    return list
}

fun Block.isDifferentState(other: Block): Boolean {
    val pattern = this.knownProperties ?: return false
    val values = other.knownProperties ?: arrayOf()
    val size = minOf(pattern.size, values.size)
    for (i in 0 until size) {
        val value = pattern[i] ?: continue
        if (value != values[i]) return true
    }
    return false
}

inline fun <reified T : Parcelable> Bundle.getTypedParcelable(key: String): T? =
    BundleCompat.getParcelable(this, key, T::class.java)

inline fun <reified T : Serializable> Intent.getTypedSerializableExtra(key: String): T? =
    IntentCompat.getSerializableExtra(this, key, T::class.java)