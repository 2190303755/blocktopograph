package com.mithrilmania.blocktopograph.nbt.io

import com.mithrilmania.blocktopograph.util.writeIntLE
import java.io.Closeable
import java.io.DataOutput
import java.io.DataOutputStream
import java.io.OutputStream
import java.lang.Double.doubleToLongBits
import java.lang.Float.floatToIntBits

interface NBTOutput : DataOutput, Closeable

class JavaNBTOutput(
    val stream: DataOutputStream
) : NBTOutput, DataOutput by stream, Closeable by stream {
    constructor(stream: OutputStream) : this(DataOutputStream(stream))
}

class BedrockNBTOutput(
    val stream: OutputStream
) : NBTOutput, Closeable by stream {
    override fun write(b: Int) {
        this.stream.write(b)
    }

    override fun write(b: ByteArray) {
        this.write(b, 0, b.size)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        this.stream.write(b, off, len)
    }

    override fun writeBoolean(v: Boolean) {
        this.stream.write(if (v) 1 else 0)
    }

    override fun writeByte(v: Int) {
        this.stream.write(v)
    }

    override fun writeShort(v: Int) {
        this.stream.write(v)
        this.stream.write(v shr 8)
    }

    override fun writeChar(v: Int) {
        this.stream.write(v)
        this.stream.write(v shr 8)
    }

    override fun writeInt(v: Int) {
        this.stream.writeIntLE(v)
    }

    override fun writeLong(v: Long) {
        this.stream.apply {
            write((v).toInt())
            write((v shr 8).toInt())
            write((v shr 16).toInt())
            write((v shr 24).toInt())
            write((v shr 32).toInt())
            write((v shr 40).toInt())
            write((v shr 48).toInt())
            write((v shr 56).toInt())
        }
    }

    override fun writeFloat(v: Float) {
        this.writeInt(floatToIntBits(v))
    }

    override fun writeDouble(v: Double) {
        this.writeLong(doubleToLongBits(v))
    }

    override fun writeBytes(s: String) {
        s.forEach {
            this.stream.write(it.code)
        }
    }

    override fun writeChars(s: String) {
        s.forEach {
            val v = it.code
            this.stream.write(v)
            this.stream.write(v shr 8)
        }
    }

    override fun writeUTF(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        // TODO: check size
        this.writeShort(bytes.size)
        this.write(bytes)
    }
}