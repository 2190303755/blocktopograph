package com.mithrilmania.blocktopograph.world

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_TITLE
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.FilteredCompound
import com.mithrilmania.blocktopograph.nbt.io.putSimpleFilter
import com.mithrilmania.blocktopograph.nbt.io.skipString
import com.mithrilmania.blocktopograph.storage.Location
import java.io.InputStream

class WorldDetail(
    val location: Location,
    val config: Location,
    val name: String,
    val mode: String,
    val time: Long,
    val seed: String,
    val version: String,
    val tag: String
) {
    var behaviors: Int by mutableIntStateOf(0)
    var resources: Int by mutableIntStateOf(0)
    var icon: Bitmap? by mutableStateOf(null)
    var size: String? by mutableStateOf(null)

    fun applyTo(intent: Intent) = this.location.applyTo(intent)
        .putExtra(EXTRA_TITLE, this.name)
}

class WorldStatistics(
    val behaviors: Int,
    val resources: Int,
    val size: Long
)

fun InputStream.extractDetail(
    location: Location,
    config: Location,
    context: Context,
    tag: String = ""
): WorldDetail? {
    var name: String? = null
    var mode: String? = null
    var time = 0L
    var seed: String? = null
    var version: String? = null
    try {
        BedrockNBTInput(this.buffered()).use { input ->
            input.skipBytes(8)
            if (input.readByte() == TAG_COMPOUND) {
                input.skipString()
                val compound = FilteredCompound {
                    putSimpleFilter(StringTag.Type, KEY_LEVEL_NAME)
                    putSimpleFilter(IntTag.Type, KEY_GAME_MODE)
                    putSimpleFilter(LongTag.Type, KEY_LAST_PLAYED_TIME, KEY_RANDOM_SEED)
                    putSimpleFilter(ListTag.Type, KEY_LAST_PLAYED_VERSION)
                }.read(input)
                (compound[KEY_LEVEL_NAME] as? StringTag)?.let {
                    name = it.value
                }
                (compound[KEY_GAME_MODE] as? IntTag)?.let {
                    mode = when (it.value) {
                        0 -> context.getString(R.string.game_mode_survival)
                        1 -> context.getString(R.string.game_mode_creative)
                        2 -> context.getString(R.string.game_mode_adventure)
                        6 -> context.getString(R.string.game_mode_spectator)
                        else -> context.getString(R.string.game_mode_unknown, it.toString())
                    }
                }
                (compound[KEY_LAST_PLAYED_TIME] as? LongTag)?.let {
                    time = it.toLong() * 1000L
                }
                (compound[KEY_RANDOM_SEED] as? LongTag)?.let {
                    seed = it.toLong().toString()
                }
                (compound[KEY_LAST_PLAYED_VERSION] as? ListTag)?.let {
                    version = it.joinToString(separator = ".")
                }
            }
        }
    } catch (_: Exception) {
        return null
    }
    val unknown by lazy { context.getString(R.string.generic_unknown) }
    return WorldDetail(
        location,
        config,
        name ?: location.queryName(context),
        mode ?: unknown,
        time,
        seed ?: unknown,
        version ?: unknown,
        tag
    )
}