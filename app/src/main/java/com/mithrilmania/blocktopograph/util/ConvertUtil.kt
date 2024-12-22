package com.mithrilmania.blocktopograph.util

import android.content.Context
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.tags.IntTag
import com.mithrilmania.blocktopograph.nbt.tags.Tag

fun Tag<*>?.getGameMode(context: Context) = when ((this as? IntTag)?.value) {
    0 -> context.getString(R.string.gamemode_survival)
    1 -> context.getString(R.string.gamemode_creative)
    2 -> context.getString(R.string.gamemode_adventure)
    else -> "Unknown"
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