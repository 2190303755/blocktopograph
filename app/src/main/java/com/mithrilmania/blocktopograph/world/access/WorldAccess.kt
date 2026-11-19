package com.mithrilmania.blocktopograph.world.access

import android.content.Context
import android.net.Uri
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.MarkerManager
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CollectionTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readAnonymousTypedTag
import com.mithrilmania.blocktopograph.nbt.util.resolveVec3f
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.storage.Workspace
import com.mithrilmania.blocktopograph.util.math.DimensionVec3f
import com.mithrilmania.blocktopograph.util.math.DimensionVector3
import com.mithrilmania.blocktopograph.util.runSuppressing
import com.mithrilmania.blocktopograph.world.CustomDimension
import com.mithrilmania.blocktopograph.world.KEY_RANDOM_SEED
import com.mithrilmania.blocktopograph.world.StandaloneKey
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.WorldConfig
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.get
import com.mithrilmania.blocktopograph.world.toVanillaDimension
import kotlinx.coroutines.runBlocking
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options

abstract class WorldAccess(@JvmField val name: String?, config: File, context: Context) {
    @JvmField
    val appContext: Context = context.applicationContext
    @JvmField
    val config: WorldConfig = WorldConfig(config, context)
    abstract val identifier: String
    abstract val markers: MarkerManager

    /**
     * Looks up the previously prepared workspace for this world and validates it against the
     * source MANIFEST modification time. Returns the usable workspace, or `null` when there is
     * no record or the prepared copy is stale.
     */
    abstract suspend fun queryPreparedWorkspaceAndValidate(): Workspace?

    /**
     * Creates a fresh prepared workspace, discarding any previous one. Returns `null` on failure.
     */
    abstract suspend fun prepareWorkspace(): Workspace?

    /**
     * Legacy convenience: reuses a valid workspace or prepares a fresh one, then opens it as a
     * [WorldStorage]. New code should use [queryPreparedWorkspaceAndValidate]/[prepareWorkspace]
     * directly so it can interact with the user between the two steps.
     */
    open suspend fun open(): WorldStorage? = (queryPreparedWorkspaceAndValidate() ?: prepareWorkspace())
        ?.let { WorldStorage(it.file.path, Options.newDefaultOptions()) }

    /**
     * copy the changed leveldb into the world.
     *
     * Returns the `Uri` of the exported `.mcworld` archive when the world
     * folder is not writable, or `null` when the changes were written in place.
     */
    abstract suspend fun merge(leveldb: String): Uri?
}

private val FORMATTER: Regex = Regex("§.")
fun WorldAccess.resolvePlainName(context: Context): String = this.name?.replace(FORMATTER, "")
    ?: context.getString(R.string.world_default_name)

fun WorldAccess.resolveSeed(): Long {
    return runBlocking {
        this@resolveSeed.config.getCached()
    }.getTyped<NumericTag>(KEY_RANDOM_SEED)?.toLong() ?: 0L
}

fun WorldAccess.resolveSpawnPoint(storage: WorldStorage?): DimensionVector3<Int> {
    val tags = runBlocking { this@resolveSpawnPoint.config.getCached() }
    val spawnX = tags.getTyped<NumericTag>("SpawnX")
    val spawnY = tags.getTyped<NumericTag>("SpawnY")
    val spawnZ = tags.getTyped<NumericTag>("SpawnZ")
    if (spawnX !== null && spawnY !== null && spawnZ !== null) {
        val x = spawnX.toInt()
        var y = spawnY.toInt()
        val z = spawnZ.toInt()
        if (y >= 256) runSuppressing {
            val chunk = storage?.getChunk(x shr 4, z shr 4, VanillaDimension.OVERWORLD)
            if (chunk !== null && !chunk.isError) {
                y = chunk.getHeightMapValue(x and 0xF, z and 0xF) + 1
            }
        }
        return DimensionVector3(x, y, z, VanillaDimension.OVERWORLD)
    }
    throw NullPointerException("Could not find spawn")
}

suspend fun WorldAccess.resolveLocalPlayerPos(db: DB?): DimensionVec3f? {
    try {
        val data: ByteArray? = db?.get(StandaloneKey.LOCAL_PLAYER)
        val player: BinaryTag? = if (data === null) {
            this@resolveLocalPlayerPos.config.getCached()["Player"]
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

suspend fun WorldAccess.resolveLocalPlayerPos(storage: WorldStorage?): DimensionVec3f? =
    resolveLocalPlayerPos(storage?.db)

fun WorldAccess.resolveLocalPlayerPosCompat(storage: WorldStorage?) = runBlocking {
    resolveLocalPlayerPos(storage)
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