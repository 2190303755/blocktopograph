package com.mithrilmania.blocktopograph.world

import android.content.Intent
import android.content.Intent.EXTRA_TITLE
import android.graphics.Bitmap
import com.mithrilmania.blocktopograph.storage.Location

class WorldInfo(
    val location: Location,
    val config: Location,
    val name: String,
    val mode: String,
    val time: Long,
    val seed: String,
    val version: String,
    val tag: String
) {
    val path: String = location.location
    var behavior: Int = 0
    var resource: Int = 0
    var icon: Bitmap? = null
    var size: String? = null

    fun applyTo(intent: Intent) = this.location.applyTo(intent)
        .putExtra(EXTRA_TITLE, this.name)
}