package com.mithrilmania.blocktopograph.world

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_TITLE
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CollectionTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readNamedTag
import com.mithrilmania.blocktopograph.nbt.io.runSuppressing
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.error
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.math.DimensionVec3f
import com.mithrilmania.blocktopograph.util.math.DimensionVector3
import com.mithrilmania.blocktopograph.util.toLDBKey
import com.mithrilmania.blocktopograph.world.impl.SAFWorld
import com.mithrilmania.blocktopograph.world.impl.ShizukuWorld
import kotlinx.coroutines.Deferred
import java.io.ByteArrayInputStream
import java.io.Closeable

abstract class World(name: String?, config: File) : Closeable {
    val plainName = name?.replace(FORMATTER, "") ?: "My World"
    val config: WorldConfig = WorldConfig(config)
    var storage: WorldStorage? = null
        private set

    /**
     * try open leveldb if [storage] is `null`
     */
    suspend fun openWithCache(context: Context): WorldStorage? {
        val storage = this.storage
        if (storage !== null) return storage
        this.storage = this.open(context)
        return this.storage
    }

    abstract suspend fun open(context: Context): WorldStorage?

    /**
     * copy the changed leveldb into the world
     */
    abstract suspend fun sync(context: Context)

    override fun close() {
        this.storage?.close()
    }

    companion object {
        @JvmField
        val FORMATTER: Regex = Regex("§.")
    }
}

fun Intent.resolveWorld(context: Context): World? {
    val uri = this.data
    if (uri === null) {
        return ShizukuWorld(
            this.getStringExtra(EXTRA_PATH) ?: return null,
            this.getStringExtra(EXTRA_TITLE)
                ?: context.getString(R.string.world_default_name)
        )
    }
    return SAFWorld(
        uri,
        uri.findChild(context.contentResolver, FILE_LEVEL_DAT) ?: return null,
        this.getStringExtra(EXTRA_TITLE)
            ?: context.getString(R.string.world_default_name)
    )
}

suspend inline fun <T> Deferred<WorldStorage?>.await(
    action: (WorldStorage) -> T
): T? = try {
    this.await()
} catch (e: Exception) {
    e.error("Failed to open world")
    return null
}?.let(action)

fun World.resolveSeed(context: Context?): Long {
    return (this.config.getCached(context)[KEY_RANDOM_SEED] as? NumericTag)?.toLong() ?: 0
}

fun World.resolveSpawnPoint(context: Context?): DimensionVector3<Int> {
    val tag = this.config.getCached(context)
    val spawnX = tag["SpawnX"] as? NumericTag
    val spawnY = tag["SpawnY"] as? NumericTag
    val spawnZ = tag["SpawnZ"] as? NumericTag
    if (spawnX !== null && spawnY !== null && spawnZ != null) {
        val x = spawnX.toInt()
        var y = spawnY.toInt()
        val z = spawnZ.toInt()
        if (y >= 256) runSuppressing {
            val chunk = this.storage?.getChunk(x shr 4, z shr 4, VanillaDimension.OVERWORLD)
            if (chunk !== null && !chunk.isError) {
                y = chunk.getHeightMapValue(x % 16, z % 16) + 1
            }
        }
        return DimensionVector3(x, y, z, VanillaDimension.OVERWORLD)
    }
    throw ClassCastException("Could not find spawn")
}


fun World.resolveLocalPlayerPos(context: Context?): DimensionVector3<Float>? {
    try {
        val data: ByteArray? = this.storage?.db?.get(SpecialDBEntryType.LOCAL_PLAYER.keyBytes)
        val player: BinaryTag? = if (data === null) {
            this.config.getCached(context)["Player"]
        } else {
            BedrockNBTInput(ByteArrayInputStream(data)).readNamedTag().second
        }
        if (player !is CompoundTag) {
            LogUtil.d(this, "No local player. A server world?")
            return null
        }
        return player.extractPlayerPosCompat()
    } catch (e: Exception) {
        LogUtil.d(this, e)
        return null
    }
}

fun World.resolveMultiPlayerPos(key: String): DimensionVector3<Float>? {
    try {
        return this.storage?.db?.get(key.toLDBKey())?.let {
            BedrockNBTInput(ByteArrayInputStream(it)).readNamedTag().second as? CompoundTag
        }?.extractPlayerPosCompat()
    } catch (e: Exception) {
        LogUtil.d(this, e)
        return null
    }
}

fun CompoundTag.extractPlayerPosCompat(): DimensionVector3<Float>? {
    val pos = this.extractPlayerPos() ?: return null
    return DimensionVector3(
        pos.x,
        pos.y,
        pos.z,
        when (pos.dimensionId) {
            0 -> VanillaDimension.OVERWORLD
            1 -> VanillaDimension.NETHER
            2 -> VanillaDimension.END
            else -> CustomDimension("Unknown", pos.dimensionId)
        }
    )
}

fun CompoundTag.extractPlayerPos(): DimensionVec3f? {
    val dimensionId = this["DimensionId"] as? NumericTag
    val dimension = if (dimensionId === null) 0 else dimensionId.toInt()
    val pos = this["Pos"] as CollectionTag<*>
    if (pos.size != 3) return null
    return DimensionVec3f(
        dimension,
        (pos.getAsTag(0) as? NumericTag ?: return null).toFloat(),
        (pos.getAsTag(1) as? NumericTag ?: return null).toFloat(),
        (pos.getAsTag(2) as? NumericTag ?: return null).toFloat()
    )
}