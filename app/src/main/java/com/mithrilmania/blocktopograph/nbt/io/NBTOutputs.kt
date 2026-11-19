package com.mithrilmania.blocktopograph.nbt.io

import com.mithrilmania.blocktopograph.util.writeIntLE
import java.io.Closeable
import java.io.DataOutput
import java.io.DataOutputStream
import java.io.OutputStream
import java.io.UTFDataFormatException
import java.lang.Double.doubleToLongBits
import java.lang.Float.floatToIntBits

typealias JavaNBTOutput = DataOutputStream

class BedrockNBTOutput(
    @JvmField val stream: OutputStream
) : DataOutput, Closeable by stream {
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
        this.writeShort(v)
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
        val stream = this.stream
        s.forEach {
            stream.write(it.code)
        }
    }

    override fun writeChars(s: String) {
        val stream = this.stream
        s.forEach {
            val v = it.code
            stream.write(v)
            stream.write(v shr 8)
        }
    }

    override fun writeUTF(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        if (bytes.size > 65535) {
            throw UTFDataFormatException("encoded string too long: ${bytes.size} bytes")
        }
        this.writeShort(bytes.size)
        this.write(bytes)
    }
}