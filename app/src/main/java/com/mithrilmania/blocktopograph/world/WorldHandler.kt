package com.mithrilmania.blocktopograph.world

import android.content.Context
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.map.Dimension
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CollectionTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.nbt.io.readNBT
import com.mithrilmania.blocktopograph.nbt.io.readNamedTag
import com.mithrilmania.blocktopograph.nbt.io.runSuppressing
import com.mithrilmania.blocktopograph.nbt.old.tags.CompoundTag
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.error
import com.mithrilmania.blocktopograph.util.math.DimensionVector3
import com.mithrilmania.blocktopograph.util.toLDBKey
import kotlinx.coroutines.CoroutineScope
import java.io.ByteArrayInputStream
import java.io.IOException
import com.mithrilmania.blocktopograph.nbt.CompoundTag as TagCompound

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
        this.loadCompat(context)
        return this.dataCompat ?: CompoundTag("", ArrayList())
    }

    protected var data: TagCompound? = null
    fun resolveData(context: Context?): TagCompound {
        this.data?.let { return it }
        if (context === null) return TagCompound()
        this.load(context, NBTImportConfigImpl())
        return this.data ?: TagCompound()
    }

    abstract val config: File

    /**
     * Read [CompoundTag] from `level.dat` and update [dataCompat]
     */
    abstract fun loadCompat(context: Context)

    /**
     * Read [CompoundTag] from `level.dat` and update [data]
     */
    fun load(context: Context, config: NBTImportConfig) {
        try {
            this.config.read(context) { it.readNBT(config) }?.let {
                this.data = it.tag as? TagCompound
            }
        } catch (e: IOException) {
            e.error("Failed to read level.dat at ${this.config}")
        }
    }

    /**
     * try open leveldb if [storage] is `null`
     */
    abstract suspend fun open(context: Context): WorldStorage?

    /**
     * copy the changed leveldb into the world
     */
    abstract fun sync(scope: CoroutineScope, context: Context)
}

fun WorldHandler.resolveSeed(context: Context?): Long {
    return (this.resolveData(context)[KEY_RANDOM_SEED] as? NumericTag)?.toLong() ?: 0
}

fun WorldHandler.resolveSpawnPoint(context: Context?): DimensionVector3<Int> {
    val tag = this.resolveData(context)
    val spawnX = tag["SpawnX"] as? NumericTag
    val spawnY = tag["SpawnY"] as? NumericTag
    val spawnZ = tag["SpawnZ"] as? NumericTag
    if (spawnX !== null && spawnY !== null && spawnZ != null) {
        val x = spawnX.toInt()
        var y = spawnY.toInt()
        val z = spawnZ.toInt()
        if (y >= 256) runSuppressing {
            val chunk = this.storage?.getChunk(x shr 4, z shr 4, Dimension.OVERWORLD)
            if (chunk !== null && !chunk.isError) {
                y = chunk.getHeightMapValue(x % 16, z % 16) + 1
            }
        }
        return DimensionVector3(x, y, z, Dimension.OVERWORLD)
    }
    throw ClassCastException("Could not find spawn")
}


fun WorldHandler.resolveLocalPlayerPos(context: Context?): DimensionVector3<Float>? {
    try {
        val data: ByteArray? = this.storage?.db?.get(SpecialDBEntryType.LOCAL_PLAYER.keyBytes)
        val player: BinaryTag? = if (data === null) {
            this.resolveData(context)["Player"]
        } else {
            BedrockNBTInput(ByteArrayInputStream(data)).readNamedTag().second
        }
        if (player !is TagCompound) {
            LogUtil.d(this, "No local player. A server world?")
            return null
        }
        return player.extractPlayerPos()
    } catch (e: Exception) {
        LogUtil.d(this, e)
        return null
    }
}

fun WorldHandler.resolveMultiPlayerPos(key: String): DimensionVector3<Float>? {
    try {
        return this.storage?.db?.get(key.toLDBKey())?.let {
            BedrockNBTInput(ByteArrayInputStream(it)).readNamedTag().second as? TagCompound
        }?.extractPlayerPos()
    } catch (e: Exception) {
        LogUtil.d(this, e)
        return null
    }
}


fun TagCompound.extractPlayerPos(): DimensionVector3<Float>? {
    val dimensionId = this["DimensionId"] as? NumericTag
    val dimension: Dimension = if (dimensionId === null) {
        Dimension.OVERWORLD
    } else {
        Dimension.getDimension(dimensionId.toInt()) ?: Dimension.OVERWORLD
    }
    val pos = this["Pos"] as CollectionTag<*>
    if (pos.size != 3) return null
    return DimensionVector3<Float>(
        (pos.getAsTag(0) as? NumericTag)?.toFloat() ?: return null,
        (pos.getAsTag(1) as? NumericTag)?.toFloat() ?: return null,
        (pos.getAsTag(2) as? NumericTag)?.toFloat() ?: return null,
        dimension
    )
}