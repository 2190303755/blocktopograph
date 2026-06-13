package com.mithrilmania.blocktopograph.world.chunk

import androidx.collection.LruCache
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.registry.Registry
import com.mithrilmania.blocktopograph.world.WorldStorage
import it.unimi.dsi.fastutil.longs.Long2IntMap

class ChunkCache(
    @JvmField val storage: WorldStorage,
    @JvmField val bounds: Long2IntMap,
    @JvmField val blocks: Registry<BlockTemplate>,
    capacity: Int = 512
) : LruCache<ChunkPos, Chunk>(capacity) {
    override fun create(key: ChunkPos): Chunk? =
        key.resolveChunk(this.storage, this.bounds, this.blocks)
}