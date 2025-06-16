package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import android.content.Intent
import com.mithrilmania.blocktopograph.world.BUNDLE_ENTRY_NAME
import com.mithrilmania.blocktopograph.world.BUNDLE_ENTRY_PATH
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.WorldInfo

class ShizukuWorldInfo(
    location: String,
    name: String,
    mode: String,
    time: Long,
    seed: String,
    version: String,
    tag: String = ""
) : WorldInfo<String>(
    location,
    name,
    mode,
    time,
    seed,
    tag,
    location,
    version
) {
    override fun makeWorldIntent(intent: Intent) = intent
        .putExtra(BUNDLE_ENTRY_PATH, this.location)
        .putExtra(BUNDLE_ENTRY_NAME, this.name)

    override fun makeConfigIntent(context: Context, intent: Intent) = intent
        .putExtra(BUNDLE_ENTRY_PATH, this.location + '/' + FILE_LEVEL_DAT)
}