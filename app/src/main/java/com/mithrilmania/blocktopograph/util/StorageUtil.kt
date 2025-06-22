package com.mithrilmania.blocktopograph.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
import android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
import android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
import android.provider.DocumentsContract.Document.COLUMN_SIZE
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.provider.DocumentsContract.EXTRA_ORIENTATION
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import com.mithrilmania.blocktopograph.nbt.EditableNBT
import com.mithrilmania.blocktopograph.nbt.LevelDBNBT
import com.mithrilmania.blocktopograph.util.ConvertUtil.bytesToHexStr
import org.iq80.leveldb.DB
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun Uri.findChild(resolver: ContentResolver, name: String): Uri? {
    resolver.query(
        DocumentsContract.buildChildDocumentsUriUsingTree(
            this, DocumentsContract.getDocumentId(this)
        ), arrayOf(
            COLUMN_DISPLAY_NAME,
            COLUMN_DOCUMENT_ID
        ), null, null, null
    )?.use {
        while (it.moveToNext()) {
            if (name == it.getString(0)) {
                return DocumentsContract.buildDocumentUriUsingTree(this, it.getString(1))
            }
        }
    }
    return null
}

inline fun Uri.forChild(resolver: ContentResolver, action: (Uri) -> Unit) {
    resolver.query(
        DocumentsContract.buildChildDocumentsUriUsingTree(
            this, DocumentsContract.getDocumentId(this)
        ), arrayOf(
            COLUMN_DOCUMENT_ID
        ), null, null, null
    )?.use {
        while (it.moveToNext()) {
            action(DocumentsContract.buildDocumentUriUsingTree(this, it.getString(0)))
        }
    }
}

inline fun Uri.forSubFolder(resolver: ContentResolver, action: (Uri) -> Unit) {
    resolver.query(
        DocumentsContract.buildChildDocumentsUriUsingTree(
            this, DocumentsContract.getDocumentId(this)
        ), arrayOf(
            COLUMN_MIME_TYPE,
            COLUMN_DOCUMENT_ID
        ), null, null, null
    )?.use {
        while (it.moveToNext()) {
            if (!it.isNull(0) && MIME_TYPE_DIR == it.getString(0)) {
                action(DocumentsContract.buildDocumentUriUsingTree(this, it.getString(1)))
            }
        }
    }
}

fun Uri.getSize(resolver: ContentResolver): Long {
    resolver.query(
        this, arrayOf(
            COLUMN_MIME_TYPE,
            COLUMN_SIZE
        ), null, null, null
    )?.use {
        if (it.moveToFirst() && !it.isNull(0)) {
            if (MIME_TYPE_DIR == it.getString(0)) {
                var size = 0L
                this.forChild(resolver) { child ->
                    size += child.getSize(resolver)
                }
                return size
            } else if (!it.isNull(1)) {
                return it.getLong(1)
            }
        }
    }
    return 0L
}

fun Uri.readFirstLine(resolver: ContentResolver): String {
    return BufferedReader(
        InputStreamReader(
            resolver.openInputStream(this) ?: return "",
            StandardCharsets.UTF_8
        )
    ).use { it.readLine() }
}

fun Uri.copyFileTo(resolver: ContentResolver, target: File) {
    FileOutputStream(target).use { output ->
        resolver.openInputStream(this)?.use { input ->
            input.copyTo(output)
            output.flush()
        }
    }
}

fun Uri.copyFolderTo(resolver: ContentResolver, folder: File) {
    if (folder.mkdirs()) {
        resolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(
                this, DocumentsContract.getDocumentId(this)
            ), arrayOf(
                COLUMN_MIME_TYPE,
                COLUMN_DISPLAY_NAME,
                COLUMN_DOCUMENT_ID
            ), null, null, null
        )?.use {
            while (it.moveToNext()) {
                if (it.isNull(0) || it.isNull(1)) continue
                val target = File(folder, it.getString(1))
                if (MIME_TYPE_DIR == it.getString(0)) {
                    DocumentsContract.buildDocumentUriUsingTree(this, it.getString(2))
                        .copyFolderTo(resolver, target)
                } else {
                    DocumentsContract.buildDocumentUriUsingTree(this, it.getString(2))
                        .copyFileTo(resolver, target)
                }
            }
        }
    } else {
        Log.e("StorageUtil", "Failed to copy folder: " + folder.path)
    }
}

val Uri.asFolder: Uri
    get() {
        val paths = this.pathSegments
        when (paths.size) {
            4 -> if (paths[0] == "tree" && paths[2] == "document") return this
            2 -> if (paths[0] == "tree") {
                val path = paths[1]
                return Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(this.authority)
                    .appendPath("tree")
                    .appendPath(path)
                    .appendPath("document")
                    .appendPath(path).build()
            }
        }
        throw IllegalStateException()
    }

fun Uri.queryString(resolver: ContentResolver, column: String): String? {
    resolver.query(this, arrayOf(column), null, null, null)?.use {
        if (it.moveToFirst() && !it.isNull(0)) return it.getString(0)
    }
    return null
}

fun Uri.queryName(context: Context) = this.queryString(context.contentResolver, COLUMN_DISPLAY_NAME)

val File.size: Long
    get() = if (this.isDirectory) {
        var size = 0L
        this.listFiles()?.forEach { size += it.size }
        size
    } else this.length()

/**
 * @see [ContentResolver.loadThumbnail]
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun ParcelFileDescriptor.loadThumbnail(
    size: Size,
    signal: CancellationSignal? = null
): Bitmap? {
    var orientation = 0.0F
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource {
        AssetFileDescriptor(this, 0, AssetFileDescriptor.UNKNOWN_LENGTH).apply {
            orientation = extras?.getInt(EXTRA_ORIENTATION, 0)?.toFloat() ?: 0.0F
        }
    }) { decoder, info, source ->
        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
        // One last-ditch check to see if we've been canceled.
        signal?.throwIfCanceled();
        // We requested a rough thumbnail size, but the remote size may have
        // returned something giant, so defensively scale down as needed.
        val sample = info.size.let {
            (it.width / size.width).coerceAtLeast(it.height / size.height)
        }
        if (sample > 1) {
            decoder.setTargetSampleSize(sample)
        }
    }
    if (orientation == 0.0F) return bitmap
    val width = bitmap.width
    val height = bitmap.height
    return Bitmap.createBitmap(bitmap, 0, 0, width, height, Matrix().apply {
        setRotate(orientation, width / 2.0F, height / 2.0F)
    }, false)
}

const val FLAG_GRANT_ALL_URI_PERMISSION =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

/*
suspend inline fun <T> DataStore<Preferences>.setAsync(key: Preferences.Key<T>, value: T) =
    this.edit { it[key] = value }

fun <T> DataStore<Preferences>.getAsync(key: Preferences.Key<T>, default: T): Flow<T> =
    this.data.catch { emit(emptyPreferences()) }.map { it[key] ?: default }

fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>, default: T): T {
    var result = default
    runBlocking {
        this@get.data.first {
            result = it[key] ?: result
            true
        }
    }
    return result
}
*/

fun formatSize(size: Long): String {
    var temp: Double = size.toDouble()
    var count = 0
    while (temp > 1024 && count++ < 4) {
        temp /= 1024
    }
    return "%.2f %s".format(
        temp,
        when (count) {
            1 -> "KB"
            2 -> "MB"
            3 -> "GB"
            else -> "B"
        }
    )
}

fun String.toLDBKey() = this.toByteArray(Charsets.UTF_8)

inline fun DB.getAsEditableNBT(
    entry: SpecialDBEntryType,
    handler: (String) -> Unit = {}
) = this.getAsEditableNBT(entry.keyName, entry.keyBytes, handler)

inline fun DB.getAsEditableNBT(
    display: String,
    key: ByteArray = display.toLDBKey(),
    handler: (String) -> Unit = {}
): EditableNBT? {
    try {
        return LevelDBNBT.open(this, display, key)
    } catch (e: Exception) {
        val hex = bytesToHexStr(key)
        Log.e("LevelDB", "Failed to open data with key: $hex", e)
        handler(hex)
    }
    return null
}