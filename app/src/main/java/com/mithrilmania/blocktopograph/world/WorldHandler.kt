package com.mithrilmania.blocktopograph.world

import android.content.Context
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.map.Dimension
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CollectionTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readNamedTag
import com.mithrilmania.blocktopograph.nbt.io.runSuppressing
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.error
import com.mithrilmania.blocktopograph.util.math.DimensionVector3
import com.mithrilmania.blocktopograph.util.toLDBKey
import kotlinx.coroutines.Deferred
import java.io.ByteArrayInputStream
import com.mithrilmania.blocktopograph.nbt.CompoundTag as TagCompound

abstract class WorldHandler(
    name: String,
    config: File
) {
    var storage: WorldStorage? = null
        protected set
    val plainName = name.replace(FORMATTER, "")
    val config: WorldConfig = WorldConfig(config)

    /**
     * try open leveldb if [storage] is `null`
     */
    abstract suspend fun open(context: Context): WorldStorage?

    /**
     * copy the changed leveldb into the world
     */
    abstract suspend fun sync(context: Context)

    companion object {
        @JvmField
        val FORMATTER: Regex = Regex("§.")
    }
}

suspend inline fun <T> Deferred<WorldStorage?>.await(
    action: (WorldStorage) -> T
): T? = try {
    this.await()
} catch (e: Exception) {
    e.error("Failed to open world")
    return null
}?.let(action)

fun WorldHandler.resolveSeed(context: Context?): Long {
    return (this.config.getCached(context)[KEY_RANDOM_SEED] as? NumericTag)?.toLong() ?: 0
}

fun WorldHandler.resolveSpawnPoint(context: Context?): DimensionVector3<Int> {
    val tag = this.config.getCached(context)
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
            this.config.getCached(context)["Player"]
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