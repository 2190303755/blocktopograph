package com.mithrilmania.blocktopograph.world.impl

import android.content.Intent
import com.mithrilmania.blocktopograph.world.BUNDLE_ENTRY_NAME
import com.mithrilmania.blocktopograph.world.BUNDLE_ENTRY_PATH
import com.mithrilmania.blocktopograph.world.World

class ShizukuWorld(
    location: String,
    name: String,
    mode: String,
    time: Long,
    seed: String,
    tag: String = ""
) : World<String>(
    location,
    name,
    mode,
    time,
    seed,
    tag,
    location
) {
    override fun prepareIntent(intent: Intent) = intent
        .putExtra(BUNDLE_ENTRY_PATH, this.location)
        .putExtra(BUNDLE_ENTRY_NAME, this.name)
}