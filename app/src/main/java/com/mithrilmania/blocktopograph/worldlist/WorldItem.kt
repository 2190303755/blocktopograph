package com.mithrilmania.blocktopograph.worldlist

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag
import com.mithrilmania.blocktopograph.nbt.tags.IntTag
import com.mithrilmania.blocktopograph.nbt.tags.LongTag
import com.mithrilmania.blocktopograph.nbt.tags.StringTag
import com.mithrilmania.blocktopograph.util.queryString
import com.mithrilmania.blocktopograph.world.KEY_GAME_MODE
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED
import com.mithrilmania.blocktopograph.world.KEY_LEVEL_NAME

data class WorldItem(
    val location: Uri,
    var name: String = "",
    var mode: String = "",
    var time: Long = 0,
    var config: CompoundTag? = null,
    var tag: String = "",
    var icon: Bitmap? = null,
    var behavior: Int = 0,
    var resource: Int = 0,
    var size: String? = null
) {
    constructor(
        location: Uri,
        config: CompoundTag?,
        context: Context,
        lastModified: Long,
        tag: String
    ) : this(
        location,
        (config?.getChildTagByKey(KEY_LEVEL_NAME) as? StringTag)?.value ?: location.queryString(
            context.contentResolver,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        ) ?: context.getString(R.string.default_world_name),
        when ((config?.getChildTagByKey(KEY_GAME_MODE) as? IntTag)?.value) {
            0 -> context.getString(R.string.gamemode_survival)
            1 -> context.getString(R.string.gamemode_creative)
            2 -> context.getString(R.string.gamemode_adventure)
            else -> "Unknown"
        },
        (config?.getChildTagByKey(KEY_LAST_PLAYED) as? LongTag)?.value?.let {
            it * 1000L
        } ?: lastModified,
        config,
        tag
    )

    override fun hashCode(): Int = this.location.hashCode()

    override fun equals(other: Any?): Boolean =
        this === other || (other is WorldItem && this.location == other.location)
}