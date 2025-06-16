package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.net.Uri
import com.mithrilmania.blocktopograph.Blocktopograph
import org.iq80.leveldb.DB
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

interface File {
    fun <T> read(action: (InputStream) -> T): T?
    fun <T> save(action: (OutputStream) -> T): T?
}

class SAFFile(
    val context: Context,
    val uri: Uri
) : File {
    override fun <T> read(action: (InputStream) -> T): T? =
        this.context.contentResolver.openInputStream(this.uri)?.use(action)

    override fun <T> save(action: (OutputStream) -> T): T? =
        this.context.contentResolver.openOutputStream(this.uri)?.use(action)
}

class ShizukuFile(
    val path: String
) : File {
    override fun <T> read(action: (InputStream) -> T): T? =
        Blocktopograph.fileService?.getFileDescriptor(this.path)?.use {
            FileInputStream(it.fileDescriptor).use(action)
        }

    override fun <T> save(action: (OutputStream) -> T): T? =
        Blocktopograph.fileService?.getFileDescriptor(this.path)?.use {
            FileOutputStream(it.fileDescriptor).use(action)
        }
}

class VirtualFile(
    val db: DB,
    private val key: ByteArray
) : File {
    override fun <T> read(action: (InputStream) -> T): T? =
        this.db[this.key]?.let { ByteArrayInputStream(it).use(action) }

    override fun <T> save(action: (OutputStream) -> T): T? {
        val stream = ByteArrayOutputStream()
        val result = stream.use(action)
        this.db.put(this.key, stream.toByteArray())
        return result
    }
}