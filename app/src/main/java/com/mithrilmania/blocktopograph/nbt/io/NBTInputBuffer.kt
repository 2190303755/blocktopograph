package com.mithrilmania.blocktopograph.nbt.io

import androidx.annotation.IntRange
import com.google.common.base.Preconditions.checkPositionIndexes
import com.mithrilmania.blocktopograph.BUFFER_SIZE
import com.mithrilmania.blocktopograph.util.BYTE_0
import java.io.DataInput
import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NBTInputBuffer(
    val stream: InputStream,
    order: ByteOrder
) : DataInput, AutoCloseable {
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE).order(order)

    fun requires(@IntRange(from = 1, to = BUFFER_SIZE.toLong()) bytes: Int): ByteBuffer {
        val buffer = this.buffer
        if (buffer.remaining() < bytes) {
            buffer.compact()
            var pos = buffer.position()
            while (pos < bytes) {
                val read = this.stream.read(
                    buffer.array(),
                    pos,
                    buffer.capacity() - pos
                )
                if (read == -1) break
                pos += read
            }
            if (pos < bytes) throw EOFException("Not enough bytes available ($bytes / $pos)")
            buffer.position(pos)
            buffer.flip()
        }
        return buffer
    }

    override fun readFully(array: ByteArray) {
        this.readFully(array, 0, array.size)
    }

    override fun readFully(array: ByteArray, offset: Int, length: Int) {
        if (length == 0) return
        checkPositionIndexes(offset, offset + length, array.size)
        val buffer = this.buffer
        var total = 0
        while (total < length) {
            val remaining = buffer.remaining()
            val request = length - total
            if (remaining < request) {
                buffer.get(array, offset + total, remaining)
                total += remaining
                buffer.clear()
                val read = this.stream.read(
                    buffer.array(),
                    0,
                    buffer.capacity()
                )
                if (read == -1) throw EOFException()
                buffer.limit(read)
                continue
            }
            buffer.get(array, offset + total, request)
            total += request
        }
        if (total != length) throw IOException("Failed to read exactly $length bytes")
    }

    override fun skipBytes(bytes: Int): Int {
        if (bytes <= 0) return 0
        val buffer = this.buffer
        val remaining = buffer.remaining()
        var distance = bytes
        if (distance > remaining) {
            distance -= remaining
            buffer.position(buffer.limit())
        } else {
            buffer.position(distance + buffer.position())
            return bytes
        }
        while (distance > 0) {
            val skipped = this.stream.skip(distance.toLong())
            if (skipped > 0) {
                distance -= skipped.toInt()
            } else if (this.stream.read() != -1) {
                --distance
            } else return bytes - distance
        }
        return bytes
    }

    override fun readBoolean() = this.requires(1).get() != BYTE_0
    override fun readByte() = this.requires(1).get()
    override fun readUnsignedByte() = this.requires(1).get().toUByte().toInt()
    override fun readShort() = this.requires(2).short
    override fun readUnsignedShort() = this.requires(2).short.toUShort().toInt()
    override fun readChar() = this.requires(2).char
    override fun readInt() = this.requires(4).int
    override fun readLong() = this.requires(8).long
    override fun readFloat() = this.requires(4).float
    override fun readDouble() = this.requires(8).double
    override fun readUTF(): String = DataInputStream.readUTF(this)
    override fun readLine() = throw UnsupportedOperationException()
    override fun close() = this.stream.close()
}