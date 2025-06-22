package com.mithrilmania.blocktopograph.world

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_TITLE
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.EXTRA_PATH
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
                intent.getStringExtra(EXTRA_PATH) ?: return false,
                intent.getStringExtra(EXTRA_TITLE)
                    ?: context.getString(R.string.world_default_name)
            )
        } else {
            val config = uri.findChild(context.contentResolver, FILE_LEVEL_DAT) ?: return false
            this.handler = SAFWorldHandler(
                uri,
                config,
                intent.getStringExtra(EXTRA_TITLE)
                    ?: context.getString(R.string.world_default_name)
            )
        }
        return true
    }

    override fun onCleared() {
        super.onCleared()
        this.handler = null
    }
}