package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.queryName
import java.nio.file.Paths

sealed interface Location {
    val location: Any
    fun toFile(): File
    fun resolve(child: String, context: Context? = null): Location?
    fun applyTo(intent: Intent): Intent
    fun queryName(context: Context): String

    override fun toString(): String
}

class SAFLocation(@JvmField val uri: Uri) : Location {
    override val location get() = this.uri
    override fun toFile(): SAFFile = SAFFile(this.uri)
    override fun resolve(
        child: String,
        context: Context?
    ): SAFLocation? = SAFLocation(
        this.uri.findChild(
            context?.contentResolver ?: return null,
            child
        ) ?: return null
    )
    override fun queryName(context: Context) = this.uri.queryName(context) ?: ""

    override fun applyTo(intent: Intent) =
        intent.setData(this.uri)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.uri == (other as SAFLocation).uri
    }

    override fun hashCode(): Int = this.uri.hashCode()

    override fun toString(): String = this.uri.lastPathSegment ?: this.uri.toString()
}

class ShizukuLocation(@JvmField val path: String) : Location {
    override val location get() = this.path
    override fun toFile(): ShizukuFile = ShizukuFile(this.path)
    override fun resolve(
        child: String,
        context: Context?
    ): ShizukuLocation = ShizukuLocation(
        Paths.get(this.path, child).toString()
    )
    override fun queryName(context: Context): String = JvmFile(this.path).name
    override fun applyTo(intent: Intent) =
        intent.putExtra(EXTRA_PATH, this.path)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.path == (other as ShizukuLocation).path
    }

    override fun hashCode(): Int = this.path.hashCode()

    override fun toString(): String = this.path
}