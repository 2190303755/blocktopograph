package com.mithrilmania.blocktopograph.world.chunk

import android.util.Log
import androidx.compose.ui.util.fastCoerceAtLeast
import com.google.common.cache.CacheBuilder
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.registry.Registry
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.world.WorldStorage
import it.unimi.dsi.fastutil.longs.Long2IntMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class ChunkCache(
    parent: CoroutineScope,
    @JvmField val storage: WorldStorage,
    @JvmField val bounds: Long2IntMap,
    @JvmField val blocks: Registry<BlockTemplate>
) {
    private val scope = CoroutineScope(
        parent.coroutineContext + SupervisorJob() + Dispatchers.IO
    )
    private val cache = CacheBuilder.newBuilder().maximumSize(256).concurrencyLevel(
        Runtime.getRuntime().availableProcessors().fastCoerceAtLeast(4)
    ).build<ChunkPos, Chunk>()

    operator fun get(pos: ChunkPos): Chunk? {
        try {
            return cache.get(pos) {
                pos.resolveChunk(storage, bounds, blocks)
            }
        } catch (e: Exception) {
            // Only log the chunks that exist but cannot be loaded
            if (e.cause !is NoSuchChunkException) {
                Log.w(APP_TAG, "Failed to load chunk at $pos")
            }
        }
        return null
    }

    fun getAsync(pos: ChunkPos): Deferred<Chunk?> = scope.async { this@ChunkCache[pos] }
}