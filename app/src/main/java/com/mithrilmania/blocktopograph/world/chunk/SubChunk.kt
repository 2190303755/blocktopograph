package com.mithrilmania.blocktopograph.world.chunk

import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.block.KnownBlockRepr
import com.mithrilmania.blocktopograph.block.readBlockFormV1d2d13TerrainSubChunk
import com.mithrilmania.blocktopograph.block.writeBlockIntoV1d2d13TerrainSubChunk
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTOutput
import com.mithrilmania.blocktopograph.registry.Registry
import com.mithrilmania.blocktopograph.registry.getOrAssignId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

sealed interface SubChunk : ChunkComponent {
    fun getBlock(x: Int, y: Int, z: Int, layer: Int = 0): BlockTemplate
    fun setBlock(x: Int, y: Int, z: Int, block: BlockTemplate, layer: Int = 0)

    fun getBrightness(src: BrightnessSource, x: Int, y: Int, z: Int): Int
}

class SubChunkV1(
    private val format: Int,
    private val absIndex: Int, // byte or -1
    @JvmField val blocks: Registry<BlockTemplate>,
    @JvmField val major: Palette,
    @JvmField val minor: Palette
) : SubChunk {
    private var isDirty = false
    override fun getBlock(x: Int, y: Int, z: Int, layer: Int): BlockTemplate {
        return this.blocks[
            (if (layer == 1) this.minor else this.major)[x, y, z]
        ] ?: BlockTemplates.getAirTemplate()
    }

    override fun setBlock(x: Int, y: Int, z: Int, block: BlockTemplate, layer: Int) {
        (if (layer == 1) this.minor else this.major)[x, y, z] =
            this.blocks.getOrAssignId(block)
        this.isDirty = true
    }

    override fun getBrightness(src: BrightnessSource, x: Int, y: Int, z: Int): Int {
        return 0
    }

    override fun save(forced: Boolean): ByteArray? {
        if (!forced && !this.isDirty) return null
        val stream = ByteArrayOutputStream()
        BedrockNBTOutput(stream).use { out ->
            out.write(this.format)
            if (this.format == 1) {
                out.writeLayer(this.major, this.blocks)
            } else {
                val airId = this.blocks.getOrAssignId(BlockTemplates.getAirTemplate())
                val layer = this.minor.entries.any { it != airId }
                out.write(if (layer) 2 else 1)
                if (this.absIndex != -1) {
                    out.write(this.absIndex)
                }
                out.writeLayer(this.major, this.blocks)
                if (layer) out.writeLayer(this.minor, this.blocks)
            }
        }
        return stream.toByteArray()
    }

    companion object {
        fun ByteArrayInputStream.readLayer(registry: Registry<BlockTemplate>): Palette {
            val airId = registry.getOrAssignId(BlockTemplates.getAirTemplate())
            this.mark(0)
            val type = this.read()
            if (type < 0) return singletonPalette(airId)
            val input = BedrockNBTInput(this)
            val span = type shr 1
            if (span == 0) return singletonPalette(
                value = registry.getOrAssignId(BlockTemplates.getBest(input.readBlockFormV1d2d13TerrainSubChunk())),
                fallback = airId
            ) else {
                val slots = Int.SIZE_BITS / span
                if (slots == 0) return emptyPalette(airId)
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
                            .fastCoerceIn(1, (1 shl span))
                    ) {
                        registry.getOrAssignId(BlockTemplates.getBest(input.readBlockFormV1d2d13TerrainSubChunk()))
                    },
                    airId
                )
            }
        }

        fun BedrockNBTOutput.writeLayer(palette: Palette, registry: Registry<BlockTemplate>) {
            palette.pack(0)
            val entries = palette.entries
            val size = entries.size
            if (size == 0 || palette.indices[0] == 0xFF.toByte()) {
                this.write(0xFF)
                return
            }
            this.write(palette.indices)
            if (size > 1) {
                this.writeInt(size)
            }
            repeat(size) {
                val state = registry[entries[it]] ?: BlockTemplates.getAirTemplate()
                this.writeBlockIntoV1d2d13TerrainSubChunk(state.block)
            }
        }
    }
}

class SubChunkV0(
    @JvmField val data: ByteArray
) : SubChunk {
    private var isDirty = false
    override fun getBlock(x: Int, y: Int, z: Int, layer: Int): BlockTemplate {
        val index = indexAt(x, y, z)
        val compound = this.data[META_MAP_OFFSET + (index shr 1)].toInt()
        val legacy = KnownBlockRepr.getBestBlock(
            this.data[FORMAT_HEADER_SIZE + index].toInt() and 0xFF,
            if ((index and 1) == 1) (compound shr 4) and 0xFF else compound and 0xFF
        )
        val states = BlockTemplates.getOfType(legacy.identifier)
        return states.getOrNull(legacy.subId) ?: BlockTemplates.getAirTemplate()
    }

    override fun setBlock(x: Int, y: Int, z: Int, block: BlockTemplate, layer: Int) {
        val typeId = KnownBlockRepr.resolve(block.block.typeId)
        val meta = metaOf(block) // fixme
        val index = indexAt(x, y, z)
        val pos = META_MAP_OFFSET + (index shr 1)
        var compound = this.data[pos].toInt()
        compound = if ((index and 1) == 1) {
            ((meta and 0xFF) shl 4) or (compound and 0x00FF)
        } else {
            (meta and 0xFF) or (compound and 0xFF00)
        }
        this.data[FORMAT_HEADER_SIZE + index] = typeId.toByte()
        this.data[pos] = compound.toByte()
        this.isDirty = true
    }

    override fun getBrightness(src: BrightnessSource, x: Int, y: Int, z: Int): Int {
        val index = indexAt(x, y, z)
        val compound = this.data[
            (if (src == BrightnessSource.BLOCK) BLOCK_LIGHT_OFFSET else SKY_LIGHT_OFFSET) + (index shr 1)
        ].toInt()
        return if ((index and 1) == 1) (compound shr 4) and 0xF else compound and 0xF
    }

    override fun save(forced: Boolean): ByteArray? =
        if (forced || this.isDirty) {
            this.isDirty = false
            this.data
        } else null

    companion object {
        const val FORMAT_HEADER_SIZE = 1
        const val FULL_SEGMENT_SIZE = BLOCKS_PER_SUBCHUNK * Byte.SIZE_BYTES
        const val COMPACT_SEGMENT_SIZE = FULL_SEGMENT_SIZE / 2
        const val META_MAP_OFFSET = FORMAT_HEADER_SIZE + FULL_SEGMENT_SIZE
        const val SKY_LIGHT_OFFSET = META_MAP_OFFSET + COMPACT_SEGMENT_SIZE
        const val BLOCK_LIGHT_OFFSET = SKY_LIGHT_OFFSET + COMPACT_SEGMENT_SIZE
    }
}

