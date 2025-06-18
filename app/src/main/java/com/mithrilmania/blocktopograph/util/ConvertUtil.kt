package com.mithrilmania.blocktopograph.util

import android.content.Context
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag
import com.mithrilmania.blocktopograph.nbt.tags.IntTag
import com.mithrilmania.blocktopograph.nbt.tags.ListTag
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

/*
        it == INT_0 -> context.getString(R.string.activity_nbt_editor_game_mode_0)
        it == INT_1 -> context.getString(R.string.activity_nbt_editor_game_mode_1)
        it == INT_2 -> context.getString(R.string.activity_nbt_editor_game_mode_2)
        it == INT_3 && !bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_java_3)
        it == INT_3 && bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_bedrock_3)
        it == INT_4 && bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_bedrock_4)
        it == INT_5 && bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_bedrock_5)
        it == INT_6 && bedrock -> context.getString(R.string.activity_nbt_editor_game_mode_bedrock_6)
        else -> context.getString(R.string.activity_nbt_editor_game_mode_unknown, it)
*/