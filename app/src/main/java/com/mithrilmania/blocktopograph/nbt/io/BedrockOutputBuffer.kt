package com.mithrilmania.blocktopograph.nbt.io

import com.mithrilmania.blocktopograph.BUFFER_SIZE
import com.mithrilmania.blocktopograph.util.BYTE_0
import java.io.DataOutput
import java.io.OutputStream
import java.lang.AutoCloseable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BedrockOutputBuffer(
    val stream: OutputStream,
    val version: UInt
) : DataOutput, AutoCloseable {
    private val buffers = ArrayList<ByteBuffer>()
    private var buffer = expand()
    private var length: UInt = 0U

    private fun expand(): ByteBuffer {
        val buffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        this.buffers += buffer
        return buffer
    }

    private fun requires(bytes: Int) {
        if (this.buffer.remaining() < bytes) {
            this.buffer = expand()
        }
    }

    override fun write(array: ByteArray) = write(array, 0, array.size)
    override fun write(array: ByteArray, offset: Int, length: Int) {
        var remaining = length
        var available = this.buffer.remaining()
        var written = 0
        while (remaining > available) {
            this.buffer.put(array, offset + written, available)
            this.expand()
            written += available
            remaining -= available
            available = this.buffer.remaining()
        }
        this.buffer.put(array, offset + written, remaining)
        this.length += length.toUInt()
    }

    override fun write(b: Int) {
        this.requires(1)
        this.buffer.put((b and 0xFF).toByte())
        this.length += 1U
    }

    override fun writeBoolean(v: Boolean) {
        this.requires(1)
        this.buffer.put(if (v) 1 else BYTE_0)
        this.length += 1U
    }

    override fun writeByte(v: Int) {
        this.requires(1)
        this.buffer.putShort((v and 0xFF).toShort())
        this.length += 1U
    }

    override fun writeShort(v: Int) {
        this.requires(2)
        this.buffer.putShort((v and 0xFFFF).toShort())
        this.length += 2U
    }

    override fun writeChar(v: Int) {
        this.writeShort(v)
        this.length += 2U
    }

    override fun writeInt(v: Int) {
        this.requires(4)
        this.buffer.putInt(v)
        this.length += 4U
    }

    override fun writeLong(v: Long) {
        this.requires(8)
        this.buffer.putLong(v)
        this.length += 8U
    }

    override fun writeFloat(v: Float) {
        this.requires(4)
        this.buffer.putFloat(v)
        this.length += 4U
    }

    override fun writeDouble(v: Double) {
        this.requires(8)
        this.buffer.putDouble(v)
        this.length += 8U
    }

    override fun writeBytes(s: String) {
        s.forEach {
            this.writeByte(it.code)
        }
    }

    override fun writeChars(s: String) {
        s.forEach {
            this.writeChar(it.code)
        }
    }

    override fun writeUTF(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        this.writeShort(bytes.size) // unsigned
        this.write(bytes)
    }

    override fun close() {
        val version = this.version
        val length = this.length
        val stream = this.stream
        stream.write(
            byteArrayOf(
                (version and 0xFFU).toByte(),
                ((version shr 8) and 0xFFU).toByte(),
                ((version shr 16) and 0xFFU).toByte(),
                ((version shr 24) and 0xFFU).toByte(),
                (length and 0xFFU).toByte(),
                ((length shr 8) and 0xFFU).toByte(),
                ((length shr 16) and 0xFFU).toByte(),
                ((length shr 24) and 0xFFU).toByte()
            )
        )
        this.buffers.forEach {
            stream.write(it.array(), 0, it.position())
        }
        stream.close()
    }
}