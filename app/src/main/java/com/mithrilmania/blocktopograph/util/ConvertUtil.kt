package com.mithrilmania.blocktopograph.util

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RequiresApi
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.block.Block
import com.mithrilmania.blocktopograph.nbt.tags.ByteTag
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag
import com.mithrilmania.blocktopograph.nbt.tags.IntTag
import com.mithrilmania.blocktopograph.nbt.tags.ListTag
import com.mithrilmania.blocktopograph.nbt.tags.StringTag
import com.mithrilmania.blocktopograph.nbt.tags.Tag
import com.mithrilmania.blocktopograph.world.KEY_GAME_MODE
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_VERSION

const val BYTE_0 = 0.toByte()

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
        list.add(values[i].wrap(props[i].name))
    }
    custom.forEach { (key, value) ->
        list.add(value.wrap(key))
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
inline fun <reified T : Parcelable> Bundle.getSafely(key: String): T? =
    this.getParcelable(key, T::class.java)