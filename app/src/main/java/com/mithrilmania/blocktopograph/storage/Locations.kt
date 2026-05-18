package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.util.queryName

interface Location {
    val location: String
    val uid: Any
    fun applyTo(intent: Intent): Intent
    fun queryName(context: Context): String
}

class SAFLocation(val uri: Uri) : Location {
    override val uid get() = this.uri
    override val location get() = this.uri.let { it.lastPathSegment ?: it.toString() }
    override fun queryName(context: Context) = this.uri.queryName(context) ?: ""
    override fun applyTo(intent: Intent) =
        intent.setData(this.uri)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.uri == (other as SAFLocation).uri
    }

    override fun hashCode(): Int = this.uri.hashCode()
}

class ShizukuLocation(override val location: String) : Location {
    override val uid get() = this.location
    override fun queryName(context: Context): String = java.io.File(this.location).name
    override fun applyTo(intent: Intent) =
        intent.putExtra(EXTRA_PATH, this.location)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.location == (other as ShizukuLocation).location
    }

    override fun hashCode(): Int = this.location.hashCode()
}