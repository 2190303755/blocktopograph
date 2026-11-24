package com.mithrilmania.blocktopograph.world.chunk

import android.util.Log
import com.mithrilmania.blocktopograph.BuildConfig
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates.getAirTemplate
import com.mithrilmania.blocktopograph.registry.Registry
import com.mithrilmania.blocktopograph.registry.getOrAssignId
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.startsWith
import com.mithrilmania.blocktopograph.world.HeightRange
import com.mithrilmania.blocktopograph.world.LoadedWorld
import com.mithrilmania.blocktopograph.world.NO_CACHE_OPTION
import com.mithrilmania.blocktopograph.world.chunk.Data3DTerrain.Companion.Data3DTerrain
import com.mithrilmania.blocktopograph.world.chunk.SubChunkV1.Companion.readLayer
import org.iq80.leveldb.DB

class Chunk(
    @JvmField val pos: ChunkPos,
    @JvmField val format: Int,
    @JvmField val lowerBound: Int,
    @JvmField val upperBound: Int,
    @JvmField val terrain: Terrain,
    @JvmField val storage: DB,
    @JvmField val blocks: Registry<BlockTemplate>
) {
    @JvmField
    val subChunks: Array<SubChunk?> = arrayOfNulls(
        ((upperBound - 1) shr 4) - (lowerBound shr 4) + 1
    )

    private var deleted = false

    fun getSubChunk(y: Int): SubChunk? {
        if (y < this.lowerBound || y >= this.upperBound) return null
        val index = y shr 4
        val offset = index - (this.lowerBound shr 4)
        var subChunk = this.subChunks[offset]
        if (subChunk !== null) return subChunk
        if (BuildConfig.DEBUG) {
            Log.d(APP_TAG, "Trying to load sub-chunk #${offset} at ${this.pos}")
        }
        val data = this.storage[
            this.pos.buildKey(index.toByte()),
            DEFAULT_OPTION
        ]
        if (data === null) {
            if (this.terrain is SubChunk) {
                this.subChunks[offset] = this.terrain
                return this.terrain
            }
            return null
        }
        val format = data[0].toInt() and 0xFF
        subChunk = if (format == 1 || format > 7) {
            val registry = this.blocks
            val stream = data.inputStream()
            stream.skip(1L)
            val minor = format > 7 && (stream.read() and 0xFF) > 1
            val absIndex = if (format > 8) stream.read() else -1
            SubChunkV1(
                format,
                absIndex,
                registry,
                stream.readLayer(registry),
                if (minor) {
                    stream.readLayer(registry)
                } else {
                    emptyPalette(registry.getOrAssignId(getAirTemplate()))
                }
            )
        } else {
            SubChunkV0(data)
        }
        this.subChunks[offset] = subChunk
        return subChunk
    }

    fun getHeight(x: Int, z: Int): Int = this.terrain.getHeight(x, z)

    fun getTop(x: Int, z: Int): Int = this.getHeight(x, z) - 1 + this.lowerBound

    fun getBiome(x: Int, y: Int, z: Int): Int = this.terrain.getBiome(x, y, z)

    fun getBrightness(src: BrightnessSource, x: Int, y: Int, z: Int): Int {
        return (this.getSubChunk(y) ?: return 0).getBrightness(src, x, y, z)
    }

    fun getBlock(x: Int, y: Int, z: Int, layer: Int = 0): BlockTemplate {
        return this.getSubChunk(y)?.getBlock(x, y, z, layer) ?: getAirTemplate()
    }

    /**
     * Consider to use [SubChunk.setBlock] for batched operations
     */
    fun setBlock(x: Int, y: Int, z: Int, block: BlockTemplate, layer: Int) {
        if (this.deleted) return
        this.getSubChunk(y)?.setBlock(x, y, z, block, layer)
    }

    /**
     * Changes the biome of one column and marks the terrain data dirty.
     */
    fun setBiome(x: Int, z: Int, id: Int): Boolean {
        return !this.deleted && this.terrain.setBiome(x, z, id)
    }

    /** Writes all dirty terrain and subchunk data back to the database. */
    fun save() {
        if (this.deleted) return
        val pos = this.pos
        val db = this.storage
        this.terrain.save()?.let {
            val prefix = pos.buildPrefix()
            db[prefix, this.terrain.terrainTag] = it
        }
        val subChunk = this.subChunks
        for (offset in subChunk.indices) {
            subChunk[offset]?.save()?.let {
                val section = offset + (this.lowerBound shr 4)
                db.put(pos.buildKey(section.toByte()), it)
            }
        }
    }

    /** Deletes every key belonging to this chunk, like the old removeFullChunk operation. */
    fun delete() {
        if (this.deleted) return
        this.deleted = true
        val baseLength = if (this.pos.dimension == 0) 8 else 12
        val base = ByteArray(baseLength)
        this.pos.buildPrefix().copyInto(base, 0, 0, baseLength)
        this.storage.iterator(NO_CACHE_OPTION).use { iterator ->
            iterator.seek(base)
            while (iterator.hasNext()) {
                val key = iterator.next().key
                if (!key.startsWith(base)) break
                if (key.size in (baseLength + 1)..(baseLength + 3)) {
                    iterator.remove()
                }
            }
        }
    }
}

fun ChunkPos.resolveChunk(
    world: LoadedWorld,
    blocks: Registry<BlockTemplate>
): Chunk {
    val prefix = this.buildPrefix()
    // resolve format version
    val storage = world.storage
    val format = storage[prefix, ChunkTag.VERSION] ?: storage[prefix, ChunkTag.LEGACY_VERSION]
    if (format === null || format.isEmpty()) {
        throw NoSuchChunkException("Failed to resolve format of chunk")
    }
    if (BuildConfig.DEBUG) {
        Log.d(APP_TAG, "Trying to load chunk at $this")
    }
    // resolve bounds from metadata
    var hash = 0L
    storage[prefix, ChunkTag.METADATA_HASH]?.forEachIndexed { index, byte ->
        // just in case that the length is insufficient but too lazy to provide default value
        hash = hash or ((byte.toLong() and 0xFFL) shl (index * 8))
    }
    var min: Int
    var max: Int
    val bound = world.heightBounds.get(hash)
    if (bound == 0) {
        when (this.dimension) {
            0, 2 -> { // not sure if it works with Caves and Cliffs previews
                min = 0
                max = 256
            }

            1 -> {
                min = 0
                max = 128
            }

            else -> {
                min = -512
                max = 512
            }
        }
    } else {
        val boxed = HeightRange(bound)
        min = boxed.min.toInt()
        max = boxed.max.toInt()
    }
    // resolve terrain
    val terrain: Terrain
    var data = storage[prefix, ChunkTag.DATA_3D]
    if (data === null) {
        data = storage[prefix, ChunkTag.DATA_2D]
        if (data === null) {
            data = storage[prefix, ChunkTag.DATA_2D_LEGACY]
            if (data === null) {
                data = storage[prefix, ChunkTag.LEGACY_TERRAIN]
                if (data === null) {
                    throw NullPointerException("Failed to resolve terrain of chunk")
                }
                terrain = LegacyTerrain(data)
            } else {
                terrain = Data2DLegacyTerrain(data)
            }
        } else {
            terrain = Data2DTerrain(data)
        }
    } else {
        terrain = Data3DTerrain(data, min, max)
    }
    return Chunk(this, format[0].toInt() and 0xFF, min, max, terrain, storage, blocks)
}
