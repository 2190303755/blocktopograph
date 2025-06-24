package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.util.queryName

interface Location {
    val location: String
    fun applyTo(intent: Intent): Intent
    fun queryName(context: Context): String
}

@JvmInline
value class SAFLocation(val uri: Uri) : Location {
    override val location get() = this.uri.let { it.lastPathSegment ?: it.toString() }
    override fun queryName(context: Context) = this.uri.queryName(context) ?: ""
    override fun applyTo(intent: Intent) =
        intent.setData(this.uri)

}

@JvmInline
value class ShizukuLocation(override val location: String) : Location {
    override fun queryName(context: Context): String = java.io.File(this.location).name
    override fun applyTo(intent: Intent) =
        intent.putExtra(EXTRA_PATH, this.location)
}