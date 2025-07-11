package com.mithrilmania.blocktopograph.world

import android.content.Context
import android.text.format.DateFormat
import com.mithrilmania.blocktopograph.nbt.old.tags.CompoundTag
import com.mithrilmania.blocktopograph.nbt.old.tags.LongTag
import com.mithrilmania.blocktopograph.util.getGameMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

abstract class WorldHandler(
    val name: String,
    val path: String
) {
    var storage: WorldStorage? = null
        protected set
    val plainName = this.name.replace("§.", "")
    protected var dataCompat: CompoundTag? = null

    fun getDataCompat(context: Context?): CompoundTag {
        if (this.dataCompat != null) return this.dataCompat!!
        if (context == null) return CompoundTag("", ArrayList())
        this.load(context)
        return this.dataCompat ?: CompoundTag("", ArrayList())
    }

    /**
     * Read [CompoundTag] from `level.dat` and update [dataCompat]
     */
    abstract fun load(context: Context)

    /**
     * Save [CompoundTag] to `level.dat` and update [data]
     */
    abstract fun save(context: Context, data: CompoundTag)

    /**
     * try open leveldb if [storage] is `null`
     */
    abstract fun open(scope: CoroutineScope, context: Context): Deferred<WorldStorage?>

    /**
     * copy the changed leveldb into the world
     */
    abstract fun sync(scope: CoroutineScope, context: Context)

    fun getWorldSeed(context: Context?): Long =
        (this.getDataCompat(context).getChildTagByKey(KEY_RANDOM_SEED) as? LongTag)?.value ?: 0

    fun getLastPlayedTimestamp(context: Context?): Long =
        (this.getDataCompat(context).getChildTagByKey(KEY_LAST_PLAYED_TIME) as? LongTag)?.value ?: 0

    fun getFormattedLastPlayedTimestamp(context: Context?): String {
        val time = this.getLastPlayedTimestamp(context)
        if (time == 0L) return "?"
        return DateFormat.getDateFormat(context).format(time * 1000L)
    }

    fun getWorldGameMode(context: Context): String {
        return this.getDataCompat(context).getGameMode(context)
    }
}