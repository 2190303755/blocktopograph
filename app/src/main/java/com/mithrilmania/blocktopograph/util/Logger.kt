package com.mithrilmania.blocktopograph.util

import android.content.Context
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter

const val APP_TAG = "Blocktopograph"
const val LEVEL_DB_TAG = "LevelDB"

fun Throwable.error(
    message: String,
    tag: String = APP_TAG
) = Log.e(tag, message, this)

suspend fun Context.errorAndPop(
    message: String,
    throwable: Throwable,
    tag: String = APP_TAG
) {
    Log.e(tag, message, throwable)
    val message = StringWriter().let { writer ->
        PrintWriter(writer).use {
            throwable.printStackTrace(it)
        }
        writer.toString()
    }
    withContext(Dispatchers.Main) {
        MaterialAlertDialogBuilder(this@errorAndPop).setMessage(message).show()
    }
}