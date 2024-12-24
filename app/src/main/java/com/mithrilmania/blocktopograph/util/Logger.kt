package com.mithrilmania.blocktopograph.util

import android.content.Context
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter

const val LOG_TAG = "Blocktopograph"

fun logError(
    message: String,
    throwable: Throwable,
    tag: String = LOG_TAG
) = Log.e(tag, message, throwable)

suspend fun Context.popError(throwable: Throwable) {
    val message = StringWriter().let { writer ->
        PrintWriter(writer).use {
            throwable.printStackTrace(it)
        }
        writer.toString()
    }
    withContext(Dispatchers.Main) {
        MaterialAlertDialogBuilder(this@popError).setMessage(message).show()
    }
}