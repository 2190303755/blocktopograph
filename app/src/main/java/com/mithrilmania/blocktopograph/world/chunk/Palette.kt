package com.mithrilmania.blocktopograph.world.chunk

import com.mithrilmania.blocktopograph.util.readIntLE
import com.mithrilmania.blocktopograph.util.writeIntLE
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap
import java.lang.ref.SoftReference

class Palette(
    @JvmField var indices: ByteArray,
    @JvmField var entries: IntArray,
    @JvmField val fallback: Int
) {
    @JvmField
    var unpacked: IntArray? = null

    @JvmField
    var cache: SoftReference<IntArray>? = null

    operator fun get(x: Int, y: Int, z: Int): Int {
        val unpacked = cache?.get()
        if (unpacked != null) return unpacked[indexAt(x, y, z)]
        val span = (this.indices[0].toInt() and 0xFF) shr 1
        if (span != 0) {
            val slots = Int.SIZE_BITS / span
            if (slots == 0) return this.fallback
            val index = indexAt(x, y, z)
            val segment = this.indices.readIntLE(1 + index / slots * Int.SIZE_BYTES)
            val pos = (segment shr ((index % slots) * span)) and ((1 shl span) - 1)
            if (pos in this.entries.indices) return this.entries[pos]
        }
        return this.entries[0]
    }

    operator fun set(x: Int, y: Int, z: Int, value: Int) {
        var unpacked = this.cache?.get()
        if (unpacked === null) {
            unpacked = IntArray(BLOCKS_PER_SUBCHUNK)
            val indices = this.indices
            val span = (indices[0].toInt() and 0xFF) shr 1
            if (span == 0) {
                unpacked.fill(this.entries[0])
            } else {
                val slots = Int.SIZE_BITS / span
                if (slots == 0) {
                    unpacked.fill(this.fallback)
                } else {
                    val entries = this.entries
                    val mask = (1 shl span) - 1
                    var block = -1
                    var cursor = 1
                    repeat(
                        (BLOCKS_PER_SUBCHUNK - 1 + slots) / slots
                    ) {
                        var segment = indices.readIntLE(cursor)
                        repeat(slots) {
                            if (++block < BLOCKS_PER_SUBCHUNK) {
                                unpacked[block] = entries.getOrElse(segment and mask) {
                                    this.fallback
                                }
                                segment = segment shr span
                            }
                        }
                        cursor += Int.SIZE_BYTES
                    }
                }
            }
            this.cache = SoftReference(unpacked)
        }
        this.unpacked = unpacked
        unpacked[indexAt(x, y, z)] = value
    }

    fun pack(flag: Int = this.indices[0].toInt()) {
        val unpacked = this.unpacked ?: return
        val palette = Int2IntLinkedOpenHashMap()
        for (value in unpacked) {
            if (palette.containsKey(value)) continue
            palette.put(value, palette.size)
        }
        val entries = palette.keys.toIntArray()
        this.entries = entries
        val lastIndex = entries.lastIndex
        if (lastIndex > 0) {
            val span = (Int.SIZE_BITS - lastIndex.countLeadingZeroBits()).coerceAtLeast(1)
            val slots = Int.SIZE_BITS / span
            val segments = (BLOCKS_PER_SUBCHUNK + slots - 1) / slots
            val indices = ByteArray(1 + segments * Int.SIZE_BYTES)
            indices[0] = (span shl 1).or(flag and 1).toByte()
            var block = -1
            var offset = 1
            repeat(segments) {
                var segment = 0
                repeat(slots) { slot ->
                    if (++block < BLOCKS_PER_SUBCHUNK) {
                        segment = segment or (palette.get(
                            unpacked[block]
                        ) shl (span * slot))
                    }
                }
                indices.writeIntLE(segment, offset)
                offset += Int.SIZE_BYTES
            }
            this.indices = indices
        } else {
            this.indices = byteArrayOf((flag and 1).toByte())
        }
        this.unpacked = null
    }
}

fun singletonPalette(
    value: Int,
    fallback: Int = value,
    flag: Int = 0x00
): Palette = Palette(
    byteArrayOf(flag.toByte()),
    intArrayOf(value),
    fallback
)

fun emptyPalette(fallback: Int): Palette = Palette(
    byteArrayOf(0xFF.toByte()),
    intArrayOf(),
    fallback
)
