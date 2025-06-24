package com.mithrilmania.blocktopograph.nbt.io

import com.mithrilmania.blocktopograph.BUFFER_SIZE
import com.mithrilmania.blocktopograph.util.BYTE_0
import java.io.DataOutput
import java.io.OutputStream
import java.lang.AutoCloseable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NBTOutputBuffer(
    val stream: OutputStream,
    order: ByteOrder
) : DataOutput, AutoCloseable {
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE).order(order)

    private inline fun withBuffer(bytes: Int, action: ByteBuffer.() -> Unit) {
        val buffer = this.buffer
        val position = buffer.position()
        if (position + bytes > buffer.limit()) {
            this.stream.write(buffer.array(), 0, position)
            buffer.clear()
        }
        buffer.action()
    }

    override fun write(array: ByteArray) = this.write(array, 0, array.size)
    override fun write(array: ByteArray, offset: Int, length: Int) {
        if (offset < this.buffer.capacity()) {
            this.withBuffer(length) { put(array, offset, length) }
        } else {
            this.flush()
            this.stream.write(array, offset, length)
        }
    }

    override fun write(b: Int) {
        this.withBuffer(1) { put((b and 0xFF).toByte()) }
    }

    override fun writeBoolean(v: Boolean) {
        this.withBuffer(1) { put(if (v) 1 else BYTE_0) }
    }

    override fun writeByte(v: Int) {
        this.withBuffer(1) { put((v and 0xFF).toByte()) }
    }

    override fun writeShort(v: Int) {
        this.withBuffer(2) { putShort((v and 0xFFFF).toShort()) }
    }

    override fun writeChar(v: Int) {
        this.writeShort(v)
    }

    override fun writeInt(v: Int) {
        this.withBuffer(4) { putInt(v) }
    }

    override fun writeLong(v: Long) {
        this.withBuffer(8) { putLong(v) }
    }

    override fun writeFloat(v: Float) {
        this.withBuffer(4) { putFloat(v) }
    }

    override fun writeDouble(v: Double) {
        this.withBuffer(8) { putDouble(v) }
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

    fun flush() {
        val buffer = this.buffer
        if (buffer.position() > 0) {
            this.stream.write(buffer.array(), 0, buffer.position())
            buffer.clear()
        }
    }

    override fun close() {
        this.flush()
        this.stream.close()
    }
}