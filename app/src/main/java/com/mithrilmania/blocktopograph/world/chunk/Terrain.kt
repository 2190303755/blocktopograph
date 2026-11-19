package com.mithrilmania.blocktopograph.world.chunk

import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import com.google.common.primitives.Shorts
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTOutput
import com.mithrilmania.blocktopograph.util.readIntLE
import com.mithrilmania.blocktopograph.world.CHUNK_DIMENSION
import java.io.ByteArrayOutputStream

const val BLOCKS_PER_SUBCHUNK = CHUNK_DIMENSION * CHUNK_DIMENSION * 16
const val HEIGHT_MAP_SIZE = CHUNK_DIMENSION * CHUNK_DIMENSION * Short.SIZE_BYTES

sealed interface Terrain : ChunkComponent {
    val terrainTag: ChunkTag
    fun getHeight(x: Int, z: Int): Int

    fun getBiome(x: Int, y: Int, z: Int): Int

    /**
     * Changes the biome of one column in-place on the backing byte array.
     * Returns false when the terrain format does not support biome editing.
     */
    fun setBiome(x: Int, z: Int, id: Int): Boolean = false
}

abstract class InfiniteTerrain(@JvmField val data: ByteArray) : Terrain {
    override fun getHeight(x: Int, z: Int): Int {
        val index = indexAt(x, z) shl 1
        val height = Shorts.fromBytes(
            this.data[index + 1],
            this.data[index]
        ).toInt()
        return height
    }
}

class Data3DTerrain private constructor(
    heights: ByteArray,
    @JvmField val biomes: Array<Palette?>,
    @JvmField val minSection: Int
) : InfiniteTerrain(heights) {
    private var isDirty = false
    override val terrainTag: ChunkTag
        get() = ChunkTag.DATA_3D
    override fun getBiome(x: Int, y: Int, z: Int): Int {
        return (this.biomes.getOrNull((y shr 4) - this.minSection) ?: return 0)[x, y, z]
    }

    /**
     * Biomes are stored per section in DATA_3D, so replacing a column writes every layer
     * of every available section. Sections absent from the blob are left absent.
     */
    override fun setBiome(x: Int, z: Int, id: Int): Boolean {
        for (palette in this.biomes) {
            if (palette === null) continue
            for (y in 0 until CHUNK_DIMENSION) {
                palette[x, y, z] = id
            }
        }
        this.isDirty = true
        return true
    }

    override fun save(forced: Boolean): ByteArray? {
        if (!forced && !this.isDirty) return null
        val stream = ByteArrayOutputStream()
        BedrockNBTOutput(stream).use { out ->
            out.write(this.data)
            for (palette in this.biomes) {
                if (palette === null) {
                    out.write(0xFF)
                    continue
                }
                palette.pack(1)
                out.write(palette.indices)
                val entries = palette.entries
                if (entries.size > 1) {
                    out.writeInt(entries.size)
                }
                for (biomeId in entries) {
                    out.writeInt(biomeId)
                }
            }
        }
        return stream.toByteArray()
    }

    companion object {
        @JvmStatic
        fun Data3DTerrain(data: ByteArray, lowerBound: Int, upperBound: Int): Data3DTerrain {
            val minSection = lowerBound shr 4
            val maxSection = (upperBound - 1) shr 4
            val sections = arrayOfNulls<Palette>(maxSection - minSection + 1)
            var cursor = HEIGHT_MAP_SIZE
            for (section in sections.indices) {
                if (cursor >= data.size) break // ¿
                val type = data[cursor++].toInt() and 0xFF
                val span = type shr 1
                if (span == 0) {
                    sections[section] = singletonPalette(data.readIntLE(cursor), flag = type)
                    cursor += 4
                } else {
                    val marker = cursor - 1
                    val slots = Int.SIZE_BITS / span
                    if (slots == 0) continue // sections[section] <- null
                    cursor += (BLOCKS_PER_SUBCHUNK - 1 + slots) / slots * Int.SIZE_BYTES
                    val indices = data.copyOfRange(marker, cursor)
                    sections[section] = Palette(
                        indices,
                        IntArray(
                            data.readIntLE(cursor)
                                .fastCoerceAtMost(BLOCKS_PER_SUBCHUNK)
                                .fastCoerceIn(1, (1 shl span))
                        ) {
                            cursor += 4
                            data.readIntLE(cursor)
                        },
                        0
                    )
                    cursor += 4
                }
            }
            return Data3DTerrain(data.copyOfRange(0, HEIGHT_MAP_SIZE), sections, minSection)
        }
    }
}

class Data2DTerrain(data: ByteArray) : InfiniteTerrain(data) {
    private var isDirty = false
    override val terrainTag: ChunkTag
        get() = ChunkTag.DATA_2D
    override fun getBiome(x: Int, y: Int, z: Int): Int {
        val index = indexAt(x, z)
        val size = this.data.size - HEIGHT_MAP_SIZE
        if (size < 512) return this.data[HEIGHT_MAP_SIZE + index].toInt()
        val offset = index shl 2
        return Shorts.fromBytes(
            this.data[HEIGHT_MAP_SIZE + 1 + offset],
            this.data[HEIGHT_MAP_SIZE + offset]
        ).toInt()
    }

    override fun setBiome(x: Int, z: Int, id: Int): Boolean {
        val index = indexAt(x, z)
        val size = this.data.size - HEIGHT_MAP_SIZE
        if (size < 512) {
            this.data[HEIGHT_MAP_SIZE + index] = id.toByte()
        } else {
            val offset = HEIGHT_MAP_SIZE + (index shl 2)
            this.data[offset] = id.toByte()
            this.data[offset + 1] = (id shr 8).toByte()
        }
        this.isDirty = true
        return true
    }

    override fun save(forced: Boolean): ByteArray? =
        if (forced || this.isDirty) {
            this.isDirty = false
            this.data
        } else null
}

class Data2DLegacyTerrain(data: ByteArray) : InfiniteTerrain(data) {
    private var isDirty = false
    override val terrainTag: ChunkTag
        get() = ChunkTag.DATA_2D_LEGACY
    override fun getBiome(x: Int, y: Int, z: Int): Int {
        return this.data[HEIGHT_MAP_SIZE + indexAt(x, z) * 4].toInt()
    }

    override fun setBiome(x: Int, z: Int, id: Int): Boolean {
        this.data[HEIGHT_MAP_SIZE + indexAt(x, z) * 4] = id.toByte()
        this.isDirty = true
        return true
    }

    override fun save(forced: Boolean): ByteArray? =
        if (forced || this.isDirty) {
            this.isDirty = false
            this.data
        } else null
}
