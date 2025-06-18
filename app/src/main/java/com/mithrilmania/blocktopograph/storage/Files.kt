package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.net.Uri
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.util.queryName
import org.iq80.leveldb.DB
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

interface File {
    fun <T> read(context: Context, action: (InputStream) -> T): T?
    fun <T> save(context: Context, action: (OutputStream) -> T): T?
    fun getName(context: Context): String
}

class SAFFile(
    val uri: Uri
) : File {
    override fun <T> read(context: Context, action: (InputStream) -> T): T? =
        context.contentResolver.openInputStream(this.uri)?.use(action)

    override fun <T> save(context: Context, action: (OutputStream) -> T): T? =
        context.contentResolver.openOutputStream(this.uri)?.use(action)

    override fun getName(context: Context) = this.uri.queryName(context) ?: ""
}

class ShizukuFile(
    val path: String
) : File {
    override fun <T> read(context: Context, action: (InputStream) -> T): T? =
        Blocktopograph.fileService?.getFileDescriptor(this.path)?.use {
            FileInputStream(it.fileDescriptor).use(action)
        }

    override fun <T> save(context: Context, action: (OutputStream) -> T): T? =
        Blocktopograph.fileService?.getFileDescriptor(this.path)?.use {
            FileOutputStream(it.fileDescriptor).use(action)
        }

    override fun getName(context: Context) = java.io.File(this.path).name ?: ""
}

class VirtualFile(
    val db: DB,
    private val key: ByteArray,
    val name: String = ""
) : File {
    override fun <T> read(context: Context, action: (InputStream) -> T): T? =
        this.db[this.key]?.let { ByteArrayInputStream(it).use(action) }

    override fun <T> save(context: Context, action: (OutputStream) -> T): T? {
        val stream = ByteArrayOutputStream()
        val result = stream.use(action)
        this.db.put(this.key, stream.toByteArray())
        return result
    }

    override fun getName(context: Context) = this.name
}