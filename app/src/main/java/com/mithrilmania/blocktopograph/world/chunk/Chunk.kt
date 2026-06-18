package com.mithrilmania.blocktopograph.world.chunk

import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates.getAirTemplate
import com.mithrilmania.blocktopograph.registry.Registry
import com.mithrilmania.blocktopograph.world.HeightRange
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.chunk.Data3DTerrain.Companion.Data3DTerrain
import com.mithrilmania.blocktopograph.world.chunk.SubChunkV1.Companion.readLayer
import it.unimi.dsi.fastutil.longs.Long2IntMap
import java.io.ByteArrayInputStream

class Chunk(
    @JvmField val pos: ChunkPos,
    @JvmField val format: Int,
    @JvmField val lowerBound: Int,
    @JvmField val upperBound: Int,
    @JvmField val terrain: Terrain,
    @JvmField val storage: WorldStorage,
    @JvmField val blocks: Registry<BlockTemplate>
) {
    @JvmField
    val subchunks: Array<SubChunk?> = arrayOfNulls(
        ((upperBound - 1) shr 4) - (lowerBound shr 4) + 1
    )

    fun getSubChunk(y: Int): SubChunk? {
        if (y < this.lowerBound || y >= this.upperBound) return null
        val index = y shr 4
        val offset = index - (this.lowerBound shr 4)
        var subchunk = this.subchunks[offset]
        if (subchunk !== null) return subchunk
        val data = this.storage.db[
            this.pos.buildKey(index.toByte()),
            NO_CACHE_OPTION
        ]
        if (data === null) {
            if (this.terrain is SubChunk) {
                this.subchunks[offset] = this.terrain
                return this.terrain
            }
            return null
        }
        val format = data[0].toInt() and 0xFF
        subchunk = if (format == 1 || format > 7) {
            val stream = ByteArrayInputStream(data)
            stream.skip(1L)
            val minor = format > 7 && (stream.read() and 0xFF) > 2
            if (format > 8) {
                stream.skip(1L)
            }
            SubChunkV1(
                this.blocks,
                stream.readLayer(this.blocks),
                if (minor) stream.readLayer(this.blocks) else singletonPalette(this.blocks[getAirTemplate()])
            )
        } else {
            SubChunkV0(data)
        }
        this.subchunks[offset] = subchunk
        return subchunk
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
}

fun ChunkPos.resolveChunk(
    storage: WorldStorage,
    bounds: Long2IntMap,
    blocks: Registry<BlockTemplate>
): Chunk {
    val prefix = this.buildPrefix()
    val db = storage.db
    // resolve format version
    val format = db[prefix, ChunkTag.VERSION] ?: db[prefix, ChunkTag.LEGACY_VERSION]
    if (format === null || format.isEmpty()) {
        throw NoSuchChunkException("Failed to resolve format of chunk")
    }
    // resolve bounds from metadata
    var hash = 0L
    db[prefix, ChunkTag.METADATA_HASH]?.forEachIndexed { index, byte ->
        // just in case that the length is insufficient but too lazy to provide default value
        hash = hash or ((byte.toLong() and 0xFFL) shl (index * 8))
    }
    var min: Int
    var max: Int
    val bound = bounds.get(hash)
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
    var data = db[prefix, ChunkTag.DATA_3D]
    if (data === null) {
        data = db[prefix, ChunkTag.DATA_2D]
        if (data === null) {
            data = db[prefix, ChunkTag.DATA_2D_LEGACY]
            if (data === null) {
                data = db[prefix, ChunkTag.LEGACY_TERRAIN]
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
