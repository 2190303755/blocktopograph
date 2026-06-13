package com.mithrilmania.blocktopograph.world.chunk

import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.block.KnownBlockRepr
import com.mithrilmania.blocktopograph.block.readBlockFormV1d2d13TerrainSubChunk
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.registry.Registry
import java.io.ByteArrayInputStream

sealed interface SubChunk {
    fun getBlock(x: Int, y: Int, z: Int, layer: Int = 0): BlockTemplate

    fun getBrightness(src: BrightnessSource, x: Int, y: Int, z: Int): Int
}

class SubChunkV1(
    @JvmField val blocks: Registry<BlockTemplate>,
    @JvmField val major: Palette,
    @JvmField val minor: Palette
) : SubChunk {
    override fun getBlock(x: Int, y: Int, z: Int, layer: Int): BlockTemplate {
        return this.blocks[
            (if (layer == 1) this.minor else this.major)[x, y, z]
        ] ?: BlockTemplates.getAirTemplate()
    }

    override fun getBrightness(src: BrightnessSource, x: Int, y: Int, z: Int): Int {
        return 0
    }

    companion object {
        fun ByteArrayInputStream.readLayer(registry: Registry<BlockTemplate>): Palette {
            this.mark(0)
            val type = this.read()
            if (type == 0xFF || type < 0) return singletonPalette(registry[BlockTemplates.getAirTemplate()])
            val input = BedrockNBTInput(this)
            val length = type shr 1
            if (length == 0) {
                val state = BlockTemplates.getBest(input.readBlockFormV1d2d13TerrainSubChunk())
                var runtimeId = registry[state]
                if (runtimeId < 0) {
                    runtimeId = registry.register(state)
                }
                return singletonPalette(runtimeId, type)
            } else {
                val slots = Int.SIZE_BITS / length
                val indices = ByteArray(
                    1 + (BLOCKS_PER_SUBCHUNK - 1 + slots) / slots * Int.SIZE_BYTES
                )
                this.reset()
                this.read(indices)
                return Palette(
                    indices,
                    IntArray(
                        input.readInt()
                            .fastCoerceAtMost(BLOCKS_PER_SUBCHUNK)
                            .fastCoerceIn(1, (1 shl length))
                    ) {
                        val state = BlockTemplates.getBest(
                            input.readBlockFormV1d2d13TerrainSubChunk()
                        )
                        val runtimeId = registry[state]
                        if (runtimeId < 0) registry.register(state) else runtimeId
                    }
                )
            }
        }
    }
}

class SubChunkV0(@JvmField val data: ByteArray) : SubChunk {
    override fun getBlock(x: Int, y: Int, z: Int, layer: Int): BlockTemplate {
        val index = indexAt(x, y, z)
        val compound = this.data[META_MAP_OFFSET + (index shr 1)].toInt()
        val legacy = KnownBlockRepr.getBestBlock(
            this.data[index].toInt() and 0xFF,
            if ((index and 1) == 1) (compound shr 4) and 0xFF else compound and 0xFF
        )
        val states = BlockTemplates.getOfType(legacy.identifier)
        return states.getOrNull(legacy.subId) ?: BlockTemplates.getAirTemplate()
    }

    override fun getBrightness(src: BrightnessSource, x: Int, y: Int, z: Int): Int {
        val index = indexAt(x, y, z)
        val compound = this.data[
            (if (src == BrightnessSource.BLOCK) BLOCK_LIGHT_OFFSET else SKY_LIGHT_OFFSET) + (index shr 1)
        ].toInt()
        return if ((index and 1) == 1) (compound shr 4) and 0xF else compound and 0xF
    }

    companion object {
        const val FULL_SEGMENT_SIZE = BLOCKS_PER_SUBCHUNK * Byte.SIZE_BYTES
        const val COMPACT_SEGMENT_SIZE = FULL_SEGMENT_SIZE / 2
        const val META_MAP_OFFSET = FULL_SEGMENT_SIZE
        const val SKY_LIGHT_OFFSET = META_MAP_OFFSET + COMPACT_SEGMENT_SIZE
        const val BLOCK_LIGHT_OFFSET = SKY_LIGHT_OFFSET + COMPACT_SEGMENT_SIZE
    }
}
