package com.mithrilmania.blocktopograph.world.chunk

import org.iq80.leveldb.DB
import org.iq80.leveldb.ReadOptions

val DEFAULT_OPTION: ReadOptions = ReadOptions()

fun indexAt(x: Int, z: Int): Int =
    ((z and 0xF) shl 4) or (x and 0xF)

fun indexAt(x: Int, y: Int, z: Int): Int =
    ((x and 0xF) shl 8) or ((z and 0xF) shl 4) or (y and 0xF)

fun legacyIndexAt(x: Int, y: Int, z: Int): Int =
    ((x and 0xF) shl 11) or ((z and 0xF) shl 7) or (y and 0x7F)

operator fun DB.get(
    chunk: ByteArray,
    tag: ChunkTag,
    options: ReadOptions = DEFAULT_OPTION
): ByteArray? {
    chunk[chunk.size - 1] = tag.dataID
    return this[chunk, options]
}
