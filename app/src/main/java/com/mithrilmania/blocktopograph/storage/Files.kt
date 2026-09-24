package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.nbt.io.TagWithMeta
import com.mithrilmania.blocktopograph.nbt.io.readNBT
import com.mithrilmania.blocktopograph.nbt.io.writeNBT
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.queryName
import com.mithrilmania.blocktopograph.util.rpc
import com.mithrilmania.blocktopograph.util.toLDBKey
import org.iq80.leveldb.DB
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.File as JvmFile

fun JvmFile.open(mode: Int): ParcelFileDescriptor? = try {
    ParcelFileDescriptor.open(this, mode)
} catch (_: FileNotFoundException) {
    null
}

sealed interface File : NBTSource {
    suspend fun <T> read(context: Context, action: (InputStream) -> T?): T?
    suspend fun save(context: Context, action: (OutputStream) -> Unit)

    suspend fun isPresent(context: Context? = null): Boolean

    override suspend fun readNBT(
        context: Context,
        config: NBTImportConfig
    ): TagWithMeta? = this.read(context) {
        it.readNBT(config)
    }

    override suspend fun saveNBT(
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

class SAFFile(val uri: Uri) : File {
    override suspend fun isPresent(context: Context?): Boolean {
        return context?.contentResolver?.query(
            this.uri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null
        )?.use { it.count > 0 } ?: false
    }

    override suspend fun <T> read(context: Context, action: (InputStream) -> T?): T? =
        context.contentResolver.openInputStream(this.uri)?.use(action)

    override suspend fun save(context: Context, action: (OutputStream) -> Unit) {
        context.contentResolver.openOutputStream(this.uri)?.use(action)
    }

    override fun resolveName(context: Context) = this.uri.queryName(context) ?: ""

    override fun toString() = "SAFFile[$uri]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.uri == (other as SAFFile).uri
    }

    override fun hashCode(): Int = this.uri.hashCode()
}

class ShizukuFile(val path: String) : File {
    override suspend fun isPresent(context: Context?): Boolean {
        return awaitFileService()?.rpc { it.metadata(this.path) } !== null
    }

    override suspend fun <T> read(context: Context, action: (InputStream) -> T?): T? =
        awaitFileService()?.rpc {
            it.openFileDescriptor(this.path)
        }?.use {
            FileInputStream(it.fileDescriptor).use(action)
        }

    override suspend fun save(context: Context, action: (OutputStream) -> Unit) {
        awaitFileService()?.rpc {
            it.openFileDescriptor(this.path)
        }?.use {
            FileOutputStream(it.fileDescriptor).use(action)
        }
    }

    override fun resolveName(context: Context) = java.io.File(this.path).name ?: ""

    override fun toString() = "ShizukuFile[$path]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.path == (other as ShizukuFile).path
    }

    override fun hashCode(): Int = this.path.hashCode()
}

class VirtualFile(
    val db: DB,
    val name: String,
    private val key: ByteArray,
) : File, NBTImportConfig {
    override val format: NBTFormat get() = NBTFormat.LITTLE_ENDIAN
    override val header: HeaderPresence get() = HeaderPresence.UNCERTAIN
    override suspend fun isPresent(context: Context?): Boolean = this.db[this.key] !== null

    override suspend fun <T> read(context: Context, action: (InputStream) -> T?): T? =
        this.db[this.key]?.let { action(it.inputStream()) }

    override suspend fun save(context: Context, action: (OutputStream) -> Unit) {
        val stream = ByteArrayOutputStream()
        action(stream)
        this.db.put(this.key, stream.toByteArray())
    }

    override fun resolveName(context: Context) = this.name

    override fun toString() = "VirtualFile[$db <${key.toHexString()}>]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VirtualFile
        return this.db === other.db && this.name == other.name && this.key.contentEquals(other.key)
    }

    override fun hashCode(): Int {
        var result = this.db.hashCode()
        result = 31 * result + this.name.hashCode()
        result = 31 * result + this.key.contentHashCode()
        return result
    }
}

fun DB.file(
    entry: SpecialDBEntryType
): VirtualFile = VirtualFile(this, entry.keyName, entry.keyBytes)

fun DB.file(
    name: String,
    key: ByteArray = name.toLDBKey()
): VirtualFile = VirtualFile(this, name, key)