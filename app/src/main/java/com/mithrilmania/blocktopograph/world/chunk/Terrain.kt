package com.mithrilmania.blocktopograph.world.chunk

import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import com.google.common.primitives.Shorts
import com.mithrilmania.blocktopograph.editor.world.v2.CHUNK_DIMENSION
import com.mithrilmania.blocktopograph.util.readIntLE

const val BLOCKS_PER_SUBCHUNK = CHUNK_DIMENSION * CHUNK_DIMENSION * 16
const val HEIGHT_MAP_SIZE = CHUNK_DIMENSION * CHUNK_DIMENSION * Short.SIZE_BYTES

sealed interface Terrain {
    fun getHeight(x: Int, z: Int): Int

    fun getBiome(x: Int, y: Int, z: Int): Int
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
    override fun getBiome(x: Int, y: Int, z: Int): Int {
        return (this.biomes.getOrNull((y shr 4) - this.minSection) ?: return 0)[x, y, z]
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
                if (type == 0xFF) continue
                val length = type shr 1
                if (length == 0) {
                    sections[section] = singletonPalette(data.readIntLE(cursor), type)
                    cursor += 4
                } else {
                    val marker = cursor - 1
                    val slots = Int.SIZE_BITS / length
                    cursor += (BLOCKS_PER_SUBCHUNK - 1 + slots) / slots * Int.SIZE_BYTES
                    val indices = data.copyOfRange(marker, cursor)
                    sections[section] = Palette(
                        indices,
                        IntArray(
                            data.readIntLE(cursor)
                                .fastCoerceAtMost(BLOCKS_PER_SUBCHUNK)
                                .fastCoerceIn(1, (1 shl length))
                        ) {
                            cursor += 4
                            data.readIntLE(cursor)
                        }
                    )
                    cursor += 4
                }
            }
            return Data3DTerrain(data.copyOfRange(0, HEIGHT_MAP_SIZE), sections, minSection)
        }
    }
}

class Data2DTerrain(data: ByteArray) : InfiniteTerrain(data) {
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
}

class Data2DLegacyTerrain(data: ByteArray) : InfiniteTerrain(data) {
    override fun getBiome(x: Int, y: Int, z: Int): Int {
        return this.data[HEIGHT_MAP_SIZE + indexAt(x, z) * 4].toInt()
    }
}
