package com.mithrilmania.blocktopograph.world.chunk

import com.mithrilmania.blocktopograph.util.writeIntLE

data class ChunkPos(
    @JvmField val dimension: Int,
    @JvmField val chunkX: Int,
    @JvmField val chunkZ: Int
) {
    fun buildPrefix(): ByteArray {
        val key: ByteArray
        if (this.dimension == 0) {
            key = ByteArray(9)
        } else {
            key = ByteArray(13)
            key.writeIntLE(this.dimension, 8)
        }
        key.writeIntLE(this.chunkX, 0)
        key.writeIntLE(this.chunkZ, 4)
        return key
    }

    fun buildKey(subchunk: Byte): ByteArray {
        val key: ByteArray
        if (this.dimension == 0) {
            key = ByteArray(10)
            key[8] = ChunkTag.SUB_CHUNK_PREFIX.dataID
            key[9] = subchunk
        } else {
            key = ByteArray(14)
            key.writeIntLE(this.dimension, 8)
            key[12] = ChunkTag.SUB_CHUNK_PREFIX.dataID
            key[13] = subchunk
        }
        key.writeIntLE(this.chunkX, 0)
        key.writeIntLE(this.chunkZ, 4)
        return key
    }

    override fun toString(): String {
        return "ChunkPos[$chunkX, $chunkZ; #$dimension]"
    }
}