package com.mithrilmania.blocktopograph.util

import java.io.OutputStream

fun OutputStream.writeIntLE(value: Int) {
    this.write(value)
    this.write(value shr 8)
    this.write(value shr 16)
    this.write(value shr 24)
}

fun ByteArray.writeIntLE(value: Int, offset: Int = 0) {
    this[offset] = value.toByte()
    this[offset + 1] = (value shr 8).toByte()
    this[offset + 2] = (value shr 16).toByte()
    this[offset + 3] = (value shr 24).toByte()
}

/**
 * @see com.google.common.primitives.Ints.fromBytes
 */
fun ByteArray.readIntLE(offset: Int = 0): Int {
    return (this[offset + 3].toInt() shl 24)
        .or((this[offset + 2].toInt() and 0xFF) shl 16)
        .or((this[offset + 1].toInt() and 0xFF) shl 8)
        .or(this[offset].toInt() and 0xFF)
}
