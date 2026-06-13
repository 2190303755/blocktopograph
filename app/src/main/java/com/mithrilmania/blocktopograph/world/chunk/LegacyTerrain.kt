package com.mithrilmania.blocktopograph.world.chunk

import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.block.KnownBlockRepr
import com.mithrilmania.blocktopograph.editor.world.v2.CHUNK_DIMENSION

class LegacyTerrain(
    @JvmField val terrain: ByteArray
) : Terrain, SubChunk {
    override fun getBlock(x: Int, y: Int, z: Int, layer: Int): BlockTemplate {
        val index = legacyIndexAt(x, y, z)
        val compound = this.terrain[META_MAP_OFFSET + (index shr 1)].toInt()
        val legacy = KnownBlockRepr.getBestBlock(
            this.terrain[index].toInt() and 0xFF,
            if ((index and 1) == 1) (compound shr 4) and 0xFF else compound and 0xFF
        )
        val states = BlockTemplates.getOfType(legacy.identifier)
        return states.getOrNull(legacy.subId) ?: BlockTemplates.getAirTemplate()
    }

    override fun getBrightness(src: BrightnessSource, x: Int, y: Int, z: Int): Int {
        val index = legacyIndexAt(x, y, z)
        val compound = this.terrain[
            (if (src == BrightnessSource.BLOCK) BLOCK_LIGHT_OFFSET else SKY_LIGHT_OFFSET) + (index shr 1)
        ].toInt()
        return if ((index and 1) == 1) (compound shr 4) and 0xF else compound and 0xF
    }

    override fun getHeight(x: Int, z: Int): Int {
        return this.terrain[HEIGHT_MAP_OFFSET + indexAt(x, z)].toInt()
    }

    override fun getBiome(x: Int, y: Int, z: Int): Int {
        return this.terrain[BIOME_MAP_OFFSET + indexAt(x, z) * 4].toInt()
    }

    companion object {
        const val BLOCKS_PER_LEGACY_TERRAIN = CHUNK_DIMENSION * CHUNK_DIMENSION * 128
        const val LEGACY_HEIGHT_MAP_SIZE = CHUNK_DIMENSION * CHUNK_DIMENSION * Byte.SIZE_BYTES
        const val FULL_SEGMENT_SIZE = BLOCKS_PER_LEGACY_TERRAIN * Byte.SIZE_BYTES
        const val COMPACT_SEGMENT_SIZE = FULL_SEGMENT_SIZE / 2
        const val META_MAP_OFFSET = FULL_SEGMENT_SIZE
        const val SKY_LIGHT_OFFSET = META_MAP_OFFSET + COMPACT_SEGMENT_SIZE
        const val BLOCK_LIGHT_OFFSET = SKY_LIGHT_OFFSET + COMPACT_SEGMENT_SIZE
        const val HEIGHT_MAP_OFFSET = BLOCK_LIGHT_OFFSET + COMPACT_SEGMENT_SIZE
        const val BIOME_MAP_OFFSET = HEIGHT_MAP_OFFSET + LEGACY_HEIGHT_MAP_SIZE
    }
}