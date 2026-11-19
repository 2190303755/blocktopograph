package com.mithrilmania.blocktopograph.world

import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readAnonymousTypedTag
import com.mithrilmania.blocktopograph.nbt.io.readNBT
import com.mithrilmania.blocktopograph.util.math.DimensionVec3f
import com.mithrilmania.blocktopograph.util.math.DimensionVec3i
import com.mithrilmania.blocktopograph.world.access.WorldAccess
import com.mithrilmania.blocktopograph.world.access.extractPlayerPos
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2IntMap
import it.unimi.dsi.fastutil.longs.Long2IntMaps
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.iq80.leveldb.DB
import java.io.Closeable

/**
 * An opened world backed by the raw prepared leveldb [storage]. Replaces [WorldStorage] for the
 * editor stack; the world's source is kept as [access] for config/markers, and [workspacePath]
 * is the prepared folder (used when merging edits back into the world).
 */
class LoadedWorld(
    @JvmField val access: WorldAccess,
    @JvmField val workspacePath: String,
    @JvmField val storage: DB,
    @JvmField var spawnPos: DimensionVec3i,
    @JvmField val dimensions: Int2ObjectMap<out Dimension>,
    @JvmField val heightBounds: Long2IntMap,
    @JvmField var localPlayer: DimensionVec3f?
) : Closeable by storage

/**
 * Reads the world metadata ([StandaloneKey.DIMENSION_REGISTRY], [StandaloneKey.CHUNK_METAS], spawn point
 * and local player) that the editor needs, so the editor ViewModel does no database work itself.
 */
suspend fun resolveLoadedWorld(
    access: WorldAccess,
    workspacePath: String,
    db: DB
): LoadedWorld = coroutineScope {
    val dimensions = async(Dispatchers.IO) {
        buildDimensionRegistry(db[StandaloneKey.DIMENSION_REGISTRY])
    }
    val heightBounds = async(Dispatchers.IO) {
        val bytes = db[StandaloneKey.CHUNK_METAS]
        if (bytes === null) Long2IntMaps.EMPTY_MAP else {
            val input = BedrockNBTInput(bytes)
            val size = input.readInt()
            val ranges = Long2IntOpenHashMap(size)
            repeat(size) {
                val hash = input.readLong()
                val range = (input.readNBT().second as? CompoundTag)
                    ?.get("LastSavedDimensionHeightRange")
                if (range is CompoundTag) {
                    ranges.put(
                        hash,
                        HeightRange(
                            range.getTyped<NumericTag>("min")?.toInt() ?: 0,
                            range.getTyped<NumericTag>("max")?.toInt() ?: 0
                        ).packed
                    )
                }
            }
            ranges
        }
    }
    val spawnPos = async(Dispatchers.IO) {
        val root = access.config.getCached()
        DimensionVec3i(
            VanillaDimension.OVERWORLD.runtimeId,
            root.getTyped<NumericTag>("SpawnX")?.toInt() ?: 0,
            root.getTyped<NumericTag>("SpawnY")?.toInt() ?: 0,
            root.getTyped<NumericTag>("SpawnZ")?.toInt() ?: 0
        )
    }
    val localPlayer = async(Dispatchers.IO) {
        try {
            val bytes: ByteArray? = db[StandaloneKey.LOCAL_PLAYER]
            val player: BinaryTag? = if (bytes === null) {
                access.config.getCached()["Player"]
            } else {
                BedrockNBTInput(bytes).readAnonymousTypedTag()
            }
            if (player is CompoundTag) {
                player.extractPlayerPos()
            } else {
                LogUtil.d(this, "No local player. A server world?")
                null
            }
        } catch (e: Exception) {
            LogUtil.d(this, e)
            null
        }
    }
    LoadedWorld(
        access,
        workspacePath,
        db,
        spawnPos.await(),
        dimensions.await(),
        heightBounds.await(),
        localPlayer.await()
    )
}
