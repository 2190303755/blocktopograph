package com.mithrilmania.blocktopograph.nbt.io

import com.google.common.io.ByteStreams
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import java.io.Closeable
import java.io.DataInput
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.lang.Double.longBitsToDouble
import java.lang.Float.intBitsToFloat

interface NBTInput : DataInput, Closeable

class JavaNBTInput(
    val stream: DataInputStream
) : NBTInput, DataInput by stream, Closeable by stream {
    constructor(stream: InputStream) : this(DataInputStream(stream))
}

class BedrockNBTInput(
    val stream: InputStream
) : NBTInput, Closeable by stream {
    @Deprecated(
        message = "Unsupported Operation",
        replaceWith = ReplaceWith("java.io.BufferedReader")
    )
    override fun readLine(): Nothing {
        throw UnsupportedOperationException()
    }

    override fun readFully(b: ByteArray) {
        ByteStreams.readFully(this.stream, b)
    }

    override fun readFully(b: ByteArray, off: Int, len: Int) {
        ByteStreams.readFully(this.stream, b, off, len)
    }

    override fun skipBytes(n: Int): Int {
        return this.stream.skip(n.toLong()).toInt()
    }

    override fun readBoolean(): Boolean {
        return this.readUnsignedByte() != 0
    }

    override fun readByte(): Byte {
        val b = this.stream.read()
        if (b < 0) throw EOFException()
        return b.toByte()
    }

    override fun readShort(): Short {
        return this.readUnsignedShort().toShort()
    }

    override fun readChar(): Char {
        return this.readUnsignedShort().toChar()
    }

    override fun readInt(): Int {
        val b4 = this.readByte()
        val b3 = this.readByte()
        val b2 = this.readByte()
        val b1 = this.readByte()
        return Ints.fromBytes(b1, b2, b3, b4)
    }

    override fun readLong(): Long {
        val b8 = this.readByte()
        val b7 = this.readByte()
        val b6 = this.readByte()
        val b5 = this.readByte()
        val b4 = this.readByte()
        val b3 = this.readByte()
        val b2 = this.readByte()
        val b1 = this.readByte()
        return Longs.fromBytes(b1, b2, b3, b4, b5, b6, b7, b8)
    }

    override fun readFloat(): Float {
        return intBitsToFloat(this.readInt())
    }

    override fun readDouble(): Double {
        return longBitsToDouble(this.readLong())
    }

    override fun readUTF(): String {
        val bytes = ByteArray(this.readUnsignedShort())
        this.readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }

    override fun readUnsignedByte(): Int {
        val b = this.stream.read()
        if (b < 0) throw EOFException()
        return b
    }

    override fun readUnsignedShort(): Int {
        val b4 = this.readByte()
        val b3 = this.readByte()
        return Ints.fromBytes(0.toByte(), 0.toByte(), b3, b4)
    }
}