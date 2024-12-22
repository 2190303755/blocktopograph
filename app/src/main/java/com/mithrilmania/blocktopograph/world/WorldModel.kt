package com.mithrilmania.blocktopograph.world

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.world.impl.SAFWorldHandler
import com.mithrilmania.blocktopograph.world.impl.ShizukuWorldHandler

open class WorldModel : ViewModel() {
    var handler: WorldHandler? = null
        private set(value) {
            field?.storage?.close()
            field = value
        }

    fun init(context: Context, intent: Intent): Boolean {
        val uri = intent.data
        if (uri == null) {
            this.handler = ShizukuWorldHandler(
                intent.getStringExtra(BUNDLE_ENTRY_PATH) ?: return false,
                intent.getStringExtra(BUNDLE_ENTRY_NAME)
                    ?: context.getString(R.string.default_world_name)
            )
        } else {
            val config = uri.findChild(context.contentResolver, FILE_LEVEL_DAT) ?: return false
            this.handler = SAFWorldHandler(
                uri,
                config,
                intent.getStringExtra(BUNDLE_ENTRY_NAME)
                    ?: context.getString(R.string.default_world_name)
            )
        }
        return true
    }

    override fun onCleared() {
        super.onCleared()
        this.handler = null
    }
}