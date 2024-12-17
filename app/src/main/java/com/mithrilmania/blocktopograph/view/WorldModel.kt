package com.mithrilmania.blocktopograph.view

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.util.asFolder
import com.mithrilmania.blocktopograph.world.World

open class WorldModel : ViewModel() {
    var instance: World? = null
        private set

    open fun init(context: Context, location: Uri?): Boolean {
        this.instance = World(context, (location ?: return false).asFolder)
        return true
    }
}