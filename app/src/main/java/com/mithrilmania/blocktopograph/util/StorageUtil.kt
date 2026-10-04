package com.mithrilmania.blocktopograph.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
import android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
import android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
import android.provider.DocumentsContract.Document.COLUMN_SIZE
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.util.Log
import java.io.File
import java.io.FileOutputStream

typealias DocumentUri = Uri
typealias TreeUri = Uri

fun DocumentUri.findChild(
    resolver: ContentResolver,
    name: String
): Uri? {
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

inline fun DocumentUri.forEachChild(resolver: ContentResolver, action: (DocumentUri) -> Unit) {
    resolver.query(
        DocumentsContract.buildChildDocumentsUriUsingTree(
            this,
            DocumentsContract.getDocumentId(this)
        ), arrayOf(
            COLUMN_DOCUMENT_ID
        ), null, null, null
    )?.use {
        while (it.moveToNext()) {
            action(DocumentsContract.buildDocumentUriUsingTree(this, it.getString(0)))
        }
    }
}

fun DocumentUri.getSize(resolver: ContentResolver): Long {
    resolver.query(
        this,
        arrayOf(COLUMN_MIME_TYPE, COLUMN_SIZE),
        null,
        null,
        null
    )?.use {
        if (it.moveToFirst()) {
            if (!it.isNull(0) && MIME_TYPE_DIR == it.getString(0)) {
                var size = 0L
                this.forEachChild(resolver) { child ->
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

fun DocumentUri.copyFileTo(resolver: ContentResolver, target: File) {
    FileOutputStream(target).use { output ->
        resolver.openInputStream(this)?.use { input ->
            input.copyTo(output)
            output.flush()
        }
    }
}

fun DocumentUri.copyFolderTo(resolver: ContentResolver, folder: File) {
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

val TreeUri.toDocumentUri: DocumentUri
    get() {
        if (DocumentsContract.isTreeUri(this)) {
            val paths = this.pathSegments
            when (paths.size) {
                4 -> if (paths[2] == "document") return this
                2 -> return DocumentsContract.buildDocumentUriUsingTree(
                    this,
                    DocumentsContract.getTreeDocumentId(this)
                )
            }
        }
        throw IllegalStateException()
    }

fun Uri.getIdOfDocumentOrTreeDocument(context: Context? = null): String {
    if (context !== null && DocumentsContract.isDocumentUri(context, this)) {
        return DocumentsContract.getDocumentId(this)
    }
    return DocumentsContract.getTreeDocumentId(this)
}

fun Uri.queryString(resolver: ContentResolver, column: String): String? {
    resolver.query(this, arrayOf(column), null, null)?.use {
        if (it.moveToFirst() && !it.isNull(0)) return it.getString(0)
    }
    return null
}

fun Uri.queryName(context: Context) = this.queryString(context.contentResolver, COLUMN_DISPLAY_NAME)

val File.size: Long
    get() {
        var size = 0L
        this.walk().forEach {
            if (it.isFile) {
                size += it.length()
            }
        }
        return size
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

fun String.toLDBKey() = this.toByteArray(Charsets.UTF_8)