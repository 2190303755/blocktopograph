package com.mithrilmania.blocktopograph.world

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.DateFormat
import com.mithrilmania.blocktopograph.Log
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag
import com.mithrilmania.blocktopograph.nbt.tags.IntTag
import com.mithrilmania.blocktopograph.nbt.tags.LongTag
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.queryString
import com.mithrilmania.blocktopograph.util.readFirstLine
import java.io.IOException

class World(
    context: Context,
    val root: Uri,
    val config: Uri,
    val tag: String = ""
) {
    constructor(context: Context, root: Uri) : this(
        context,
        root,
        root.findChild(context.contentResolver, FILE_LEVEL_DAT) ?: throw NullPointerException(),
    )

    val name: String
    val plainName: String by lazy { this.name.replace("§.", "") }
    private var data: CompoundTag? = null

    init {
        val resolver = context.contentResolver
        this.name = this.root.findChild(resolver, FILE_LEVEL_NAME)
            ?.readFirstLine(resolver) /* new */
            ?: this.root.queryString(
                resolver,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME /* legacy */
            ) ?: context.getString(R.string.default_world_name)
    }

    fun getData(context: Context?): CompoundTag {
        if (this.data != null) return this.data!!
        if (context == null) return CompoundTag("", java.util.ArrayList())
        this.load(context)
        return this.data ?: CompoundTag("", java.util.ArrayList())
    }

    fun load(context: Context) {
        try {
            this.data =
                LevelDataConverter.read(context.contentResolver.openInputStream(this.config))
        } catch (e: IOException) {
            Log.e(this, e)
        }
    }

    fun save(context: Context, data: CompoundTag) {
        try {
            LevelDataConverter.write(context.contentResolver.openOutputStream(this.config), data)
        } catch (e: IOException) {
            Log.e(this, e)
        }
        this.data = data
    }

    fun getWorldSeed(context: Context?): Long =
        (this.getData(context).getChildTagByKey(KEY_RANDOM_SEED) as? LongTag)?.value ?: 0

    fun getLastPlayedTimestamp(context: Context?): Long =
        (this.getData(context).getChildTagByKey(KEY_LAST_PLAYED) as? LongTag)?.value ?: 0

    fun getFormattedLastPlayedTimestamp(context: Context?): String {
        val time = this.getLastPlayedTimestamp(context)
        if (time == 0L) return "?"
        return DateFormat.getDateFormat(context).format(time * 1000L)
    }

    fun getWorldGameMode(context: Context): String {
        return when ((this.getData(context).getChildTagByKey(KEY_GAME_MODE) as? IntTag)?.value
            ?: return "Unknown") {
            0 -> context.getString(R.string.gamemode_survival)
            1 -> context.getString(R.string.gamemode_creative)
            2 -> context.getString(R.string.gamemode_adventure)
            else -> "Unknown"
        }
    }
    /*
    *
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

    override fun equals(other: Any?): Boolean {
        return other is World && this.root == other.root
    }

    override fun hashCode(): Int {
        return this.root.hashCode()
    }
}