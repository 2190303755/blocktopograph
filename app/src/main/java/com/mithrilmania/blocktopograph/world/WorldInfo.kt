package com.mithrilmania.blocktopograph.world

import android.content.Intent
import android.graphics.Bitmap

abstract class WorldInfo<T>(
    val location: T,
    val name: String,
    val mode: String,
    val time: Long,
    val seed: String,
    val tag: String,
    val path: String,
    val version: String
) {
    var behavior: Int = 0
    var resource: Int = 0
    var icon: Bitmap? = null
    var size: String? = null
    abstract fun prepareIntent(intent: Intent): Intent

    override fun hashCode(): Int = this.location.hashCode()

    override fun equals(other: Any?): Boolean =
        this === other || (other is WorldInfo<*> && this.location == other.location)
}