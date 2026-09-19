package com.mithrilmania.blocktopograph.storage

import android.os.ParcelFileDescriptor
import okio.FileHandle
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

fun ParcelFileDescriptor.fileHandle(
    readWrite: Boolean
): FileHandle = if (readWrite) {
    ShizukuReadWriteFileHandle(this)
} else {
    ShizukuReadableFileHandle(false, this)
}

open class ShizukuReadableFileHandle(
    readWrite: Boolean,
    private val pfd: ParcelFileDescriptor
) : FileHandle(readWrite) {
    private val input: FileInputStream = FileInputStream(pfd.fileDescriptor)

    @Synchronized
    override fun protectedSize(): Long {
        return this.input.channel.size()
    }

    @Synchronized
    override fun protectedRead(
        fileOffset: Long,
        array: ByteArray,
        arrayOffset: Int,
        byteCount: Int,
    ): Int {
        val fileChannel = this.input.channel
        fileChannel.position(fileOffset)
        val byteBuffer = ByteBuffer.wrap(array, arrayOffset, byteCount)
        var bytesRead = 0
        while (bytesRead < byteCount) {
            val readResult = fileChannel.read(byteBuffer)
            if (readResult == -1) {
                if (bytesRead == 0) return -1
                break
            }
            bytesRead += readResult
        }
        return bytesRead
    }

    @Synchronized
    override fun protectedFlush() {
        this.input.channel.force(true)
    }

    @Synchronized
    override fun protectedClose() {
        this.pfd.close()
        this.input.close()
    }

    @Synchronized
    override fun protectedResize(size: Long) {
        throw UnsupportedOperationException()
    }

    @Synchronized
    override fun protectedWrite(
        fileOffset: Long,
        array: ByteArray,
        arrayOffset: Int,
        byteCount: Int,
    ) {
        throw UnsupportedOperationException()
    }
}

class ShizukuReadWriteFileHandle(
    pfd: ParcelFileDescriptor
) : ShizukuReadableFileHandle(true, pfd) {
    private val output: FileOutputStream = FileOutputStream(pfd.fileDescriptor)

    @Synchronized
    override fun protectedResize(size: Long) {
        val currentSize = size()
        val delta = size - currentSize
        if (delta > 0) {
            protectedWrite(currentSize, ByteArray(delta.toInt()), 0, delta.toInt())
        } else {
            this.output.channel.truncate(size)
        }
    }

    @Synchronized
    override fun protectedWrite(
        fileOffset: Long,
        array: ByteArray,
        arrayOffset: Int,
        byteCount: Int,
    ) {
        this.output.channel.apply {
            position(fileOffset)
            write(ByteBuffer.wrap(array, arrayOffset, byteCount))
        }
    }

    @Synchronized
    override fun protectedFlush() {
        this.output.channel.force(true)
    }

    override fun protectedClose() {
        super.protectedClose()
        this.output.close()
    }
}