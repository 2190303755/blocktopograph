package com.mithrilmania.blocktopograph.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mithrilmania.blocktopograph.Log
import org.apache.commons.io.IOUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun Uri.asFolderImpl(context: Context): DocumentFile {
    var folder = DocumentFile.fromTreeUri(context, this) ?: throw NullPointerException()
    val paths = this.pathSegments
    if (paths.size == 4) {
        paths[3].substring(folder.uri.pathSegments[3].length + 1)
            .split('/')
            .forEach {
                if (!it.isEmpty()) {
                    folder = folder.findFile(it) ?: throw NullPointerException()
                }
            }
    }
    return folder
}

fun DocumentFile.readFirstLine(context: Context): String {
    val stream = context.contentResolver.openInputStream(this.uri)
    if (stream == null) return ""
    return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readLine() }
}

fun DocumentFile.copyFileTo(context: Context, target: File) {
    FileOutputStream(target).use { output ->
        context.contentResolver.openInputStream(this.uri)?.use { input ->
            input.copyTo(output)
            output.flush()
        }
    }
}

fun DocumentFile.copyFolderTo(context: Context, folder: File) {
    if (folder.mkdirs()) {
        for (file in this.listFiles()) {
            val target =
                File(folder, file.name ?: throw NullPointerException("Failed to query file name!"))
            if (file.isDirectory) {
                file.copyFolderTo(context, target)
            } else if (target.createNewFile()) {
                file.copyFileTo(context, target)
            } else {
                Log.e(IOUtils::class, "Failed to copy file: " + target.path)
            }
        }
    } else {
        Log.e(IOUtils::class, "Failed to copy folder: " + folder.path)
    }
}

val DocumentFile.size: Long
    get() = if (this.isDirectory) {
        var size = 0L
        this.listFiles().forEach {
            size += it.size
        }
        size
    } else this.length()