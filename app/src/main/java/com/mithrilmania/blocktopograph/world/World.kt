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
import com.mithrilmania.blocktopograph.nbt.io.readAnonymousTypedTag
import com.mithrilmania.blocktopograph.nbt.util.resolveVec3f
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.error
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.math.DimensionVec3f
import com.mithrilmania.blocktopograph.util.math.DimensionVector3
import com.mithrilmania.blocktopograph.util.runSuppressing
import com.mithrilmania.blocktopograph.world.impl.SAFWorld
import com.mithrilmania.blocktopograph.world.impl.ShizukuWorld
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
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
    return runBlocking {
        this@resolveSeed.config.getCached(context)
    }.getTyped<NumericTag>(KEY_RANDOM_SEED)?.toLong() ?: 0L
}

fun World.resolveSpawnPoint(context: Context?): DimensionVector3<Int> {
    val tags = runBlocking { this@resolveSpawnPoint.config.getCached(context) }
    val spawnX = tags.getTyped<NumericTag>("SpawnX")
    val spawnY = tags.getTyped<NumericTag>("SpawnY")
    val spawnZ = tags.getTyped<NumericTag>("SpawnZ")
    if (spawnX !== null && spawnY !== null && spawnZ !== null) {
        val x = spawnX.toInt()
        var y = spawnY.toInt()
        val z = spawnZ.toInt()
        if (y >= 256) runSuppressing {
            val chunk = this.storage?.getChunk(x shr 4, z shr 4, VanillaDimension.OVERWORLD)
            if (chunk !== null && !chunk.isError) {
                y = chunk.getHeightMapValue(x and 0xF, z and 0xF) + 1
            }
        }
        return DimensionVector3(x, y, z, VanillaDimension.OVERWORLD)
    }
    throw NullPointerException("Could not find spawn")
}


suspend fun World.resolveLocalPlayerPos(context: Context?): DimensionVec3f? {
    try {
        val data: ByteArray? = this.storage?.db?.get(SpecialDBEntryType.LOCAL_PLAYER.keyBytes)
        val player: BinaryTag? = if (data === null) {
            this@resolveLocalPlayerPos.config.getCached(context)["Player"]
        } else {
            BedrockNBTInput(data).readAnonymousTypedTag()
        }
        if (player !is CompoundTag) {
            LogUtil.d(this, "No local player. A server world?")
            return null
        }
        return player.extractPlayerPos()
    } catch (e: Exception) {
        LogUtil.d(this, e)
        return null
    }
}

fun World.resolveLocalPlayerPosCompat(context: Context?) = runBlocking {
    resolveLocalPlayerPos(context)
}?.boxed()

fun DimensionVec3f.boxed(): DimensionVector3<Float> {
    return DimensionVector3(
        this.x,
        this.y,
        this.z,
        this.dimensionId.toVanillaDimension()
            ?: CustomDimension("Unknown", this.dimensionId)
    )
}

fun CompoundTag.extractPlayerPos(): DimensionVec3f? {
    val dimension = this.getTyped<NumericTag>("DimensionId")?.toInt() ?: 0
    val pos = this.getTyped<CollectionTag<*>>("Pos")?.resolveVec3f() ?: return null
    return DimensionVec3f(dimension, pos.x, pos.y, pos.z)
}