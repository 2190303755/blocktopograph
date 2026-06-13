package com.mithrilmania.blocktopograph.world.chunk

import com.mithrilmania.blocktopograph.util.readIntLE
import com.mithrilmania.blocktopograph.util.writeIntLE
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import java.lang.ref.SoftReference

class Palette(
    @JvmField var indices: ByteArray,
    @JvmField var entries: IntArray
) {
    @JvmField
    var unpacked: IntArray? = null

    @JvmField
    var cache: SoftReference<IntArray>? = null

    @JvmField
    var isDirty: Boolean = false
    operator fun get(x: Int, y: Int, z: Int): Int {
        val unpacked = this.cache?.get()
        if (unpacked !== null) return unpacked[indexAt(x, y, z)]
        val length = (this.indices[0].toInt() and 0xFF) shr 1
        if (length != 0) {
            val slots = Int.SIZE_BITS / length
            val index = indexAt(x, y, z)
            val segment = this.indices.readIntLE(1 + index / slots * Int.SIZE_BYTES)
            val pos = (segment shr ((index % slots) * length)) and ((1 shl length) - 1)
            if (pos in this.entries.indices) return this.entries[pos]
        }
        return this.entries[0]
    }

    operator fun set(x: Int, y: Int, z: Int, value: Int) {
        var unpacked = this.cache?.get()
        if (unpacked === null) {
            unpacked = IntArray(BLOCKS_PER_SUBCHUNK)
            val indices = this.indices
            val length = (indices[0].toInt() and 0xFF) shr 1
            if (length == 0) {
                unpacked.fill(this.entries[0])
            } else {
                val entries = this.entries
                val slots = Int.SIZE_BITS / length
                val mask = (1 shl length) - 1
                var block = -1
                var cursor = 1
                repeat(
                    (BLOCKS_PER_SUBCHUNK - 1 + slots) / slots
                ) {
                    var segment = indices.readIntLE(cursor)
                    repeat(slots) {
                        if (++block < BLOCKS_PER_SUBCHUNK) {
                            unpacked[block] = entries[segment and mask]
                            segment = segment shr length
                        }
                    }
                    cursor += Int.SIZE_BYTES
                }
            }
            this.cache = SoftReference(unpacked)
        }
        this.unpacked = unpacked
        unpacked[indexAt(x, y, z)] = value
        this.isDirty = true
    }

    fun pack(flag: Int) {
        val unpacked = this.unpacked ?: return
        val maxIndex = this.entries.size - 1
        if (maxIndex > 0) {
            val length = (Int.SIZE_BITS - maxIndex.countLeadingZeroBits()).coerceAtLeast(1)
            val slots = Int.SIZE_BITS / length
            val segments = (BLOCKS_PER_SUBCHUNK + slots - 1) / slots
            val indices = ByteArray(1 + segments * Int.SIZE_BYTES)
            indices[0] = (length shl 1).or(flag and 1).toByte()
            val palette = Int2IntLinkedOpenHashMap()
            palette.defaultReturnValue(-1)
            var block = -1
            var offset = 1
            repeat(segments) {
                var segment = 0
                repeat(slots) {
                    if (++block < BLOCKS_PER_SUBCHUNK) {
                        val value = unpacked[block]
                        var index = palette.get(value)
                        if (index < 0) {
                            index = palette.size
                            palette.put(value, index)
                        }
                        segment = segment or (index shl (length * it))
                    }
                }
                indices.writeIntLE(segment, offset)
                offset += Int.SIZE_BYTES
            }
            this.entries = palette.keys.toIntArray()
            this.indices = indices
        } else {
            val values = IntOpenHashSet()
            unpacked.forEach { values.add(it) }
            this.entries = values.toIntArray()
            this.indices = byteArrayOf((flag and 1).toByte())
        }
        this.unpacked = null
    }
}

fun singletonPalette(value: Int, flag: Int = 0x00): Palette = Palette(
    byteArrayOf(flag.toByte()),
    intArrayOf(value)
)