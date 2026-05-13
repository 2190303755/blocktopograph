package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.net.Uri
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.nbt.io.TagWithMeta
import com.mithrilmania.blocktopograph.nbt.io.readNBT
import com.mithrilmania.blocktopograph.nbt.io.writeNBT
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.queryName
import com.mithrilmania.blocktopograph.util.toLDBKey
import org.iq80.leveldb.DB
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

interface File : NBTSource {
    fun <T> read(context: Context, action: (InputStream) -> T?): T?
    fun save(context: Context, action: (OutputStream) -> Unit)
    override fun readNBT(
        context: Context,
        config: NBTImportConfig
    ): TagWithMeta? = this.read(context) {
        it.readNBT(config)
    }

    override fun saveNBT(
        context: Context,
        config: NBTExportConfig,
        name: String,
        tag: BinaryTag
    ) {
        this.save(context) {
            it.writeNBT(name, tag, config)
        }
    }
}

class SAFFile(
    val uri: Uri
) : File {
    override fun <T> read(context: Context, action: (InputStream) -> T?): T? =
        context.contentResolver.openInputStream(this.uri)?.use(action)

    override fun save(context: Context, action: (OutputStream) -> Unit) {
        context.contentResolver.openOutputStream(this.uri)?.use(action)
    }

    override fun resolveName(context: Context) = this.uri.queryName(context) ?: ""

    override fun toString() = "SAFFile[$uri]"
}

class ShizukuFile(
    val path: String
) : File {
    override fun <T> read(context: Context, action: (InputStream) -> T?): T? =
        Blocktopograph.fileService?.getFileDescriptor(this.path)?.use {
            FileInputStream(it.fileDescriptor).use(action)
        }

    override fun save(context: Context, action: (OutputStream) -> Unit) {
        Blocktopograph.fileService?.getFileDescriptor(this.path)?.use {
            FileOutputStream(it.fileDescriptor).use(action)
        }
    }

    override fun resolveName(context: Context) = java.io.File(this.path).name ?: ""

    override fun toString() = "ShizukuFile[$path]"
}

class VirtualFile(
    val db: DB,
    val name: String,
    private val key: ByteArray,
) : File {
    fun isPresent(): Boolean = this.db[this.key] !== null
    override fun <T> read(context: Context, action: (InputStream) -> T?): T? =
        this.db[this.key]?.let { action(ByteArrayInputStream(it)) }

    override fun save(context: Context, action: (OutputStream) -> Unit) {
        val stream = ByteArrayOutputStream()
        action(stream)
        this.db.put(this.key, stream.toByteArray())
    }

    override fun resolveName(context: Context) = this.name

    override fun toString() = "VirtualFile[$db <${key.toHexString()}>]"
}

fun DB.file(
    entry: SpecialDBEntryType
): VirtualFile = VirtualFile(this, entry.keyName, entry.keyBytes)

fun DB.file(
    name: String,
    key: ByteArray = name.toLDBKey()
): VirtualFile = VirtualFile(this, name, key)