package com.mithrilmania.blocktopograph.storage

import android.content.Intent
import android.net.Uri
import com.mithrilmania.blocktopograph.EXTRA_PATH

interface Location {
    val location: String
    fun applyTo(intent: Intent): Intent
}

data class SAFLocation(val uri: Uri) : Location {
    override val location get() = this.uri.let { it.lastPathSegment ?: it.toString() }
    override fun applyTo(intent: Intent) =
        intent.setData(this.uri)

    override fun equals(other: Any?) =
        this === other || (other is SAFLocation && this.uri == other.uri)

    override fun hashCode() = this.uri.hashCode()
}

data class ShizukuLocation(override val location: String) : Location {
    override fun applyTo(intent: Intent) =
        intent.putExtra(EXTRA_PATH, this.location)

    override fun equals(other: Any?) =
        this === other || (other is ShizukuLocation && this.location == other.location)

    override fun hashCode() = this.location.hashCode()
}