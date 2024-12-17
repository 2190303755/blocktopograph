package com.mithrilmania.blocktopograph.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.mithrilmania.blocktopograph.Log
import org.apache.commons.io.IOUtils
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
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
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
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
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
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        ), null, null, null
    )?.use {
        while (it.moveToNext()) {
            if (!it.isNull(0) && DocumentsContract.Document.MIME_TYPE_DIR == it.getString(0)) {
                action(DocumentsContract.buildDocumentUriUsingTree(this, it.getString(1)))
            }
        }
    }
}

fun Uri.getSize(resolver: ContentResolver): Long {
    resolver.query(
        this, arrayOf(
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        ), null, null, null
    )?.use {
        if (it.moveToFirst() && !it.isNull(0)) {
            if (DocumentsContract.Document.MIME_TYPE_DIR == it.getString(0)) {
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
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            ), null, null, null
        )?.use {
            while (it.moveToNext()) {
                if (it.isNull(0) || it.isNull(1)) continue
                val target = File(folder, it.getString(1))
                if (DocumentsContract.Document.MIME_TYPE_DIR == it.getString(0)) {
                    DocumentsContract.buildDocumentUriUsingTree(this, it.getString(2))
                        .copyFolderTo(resolver, target)
                } else {
                    DocumentsContract.buildDocumentUriUsingTree(this, it.getString(2))
                        .copyFileTo(resolver, target)
                }
            }
        }
    } else {
        Log.e(IOUtils::class, "Failed to copy folder: " + folder.path)
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