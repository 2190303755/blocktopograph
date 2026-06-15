package com.mithrilmania.blocktopograph.world.chunk

import androidx.collection.LruCache
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.registry.Registry
import com.mithrilmania.blocktopograph.world.WorldStorage
import it.unimi.dsi.fastutil.longs.Long2IntMap

class ChunkCache(
    @JvmField val storage: WorldStorage,
    @JvmField val bounds: Long2IntMap,
    @JvmField val blocks: Registry<BlockTemplate>
) {
    private val caches: Array<Section> = Array(SECTIONS) { Section() }

    operator fun get(key: ChunkPos): Chunk? {
        var hash = key.hashCode()
        hash = hash xor (hash shr 16) shr 8 and MASK
        return caches[hash][key]
    }

    inner class Section : LruCache<ChunkPos, Chunk>(32) {
        override fun create(key: ChunkPos): Chunk? =
            key.resolveChunk(storage, bounds, blocks)
    }

    companion object {
        const val SECTIONS = 1 shl 3
        const val MASK = SECTIONS - 1
    }
}