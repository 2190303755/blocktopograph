package com.mithrilmania.blocktopograph.util

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.mithrilmania.blocktopograph.editor.world.CreateWorldActivity
import com.mithrilmania.blocktopograph.world.WorldInfo

object WorldCreator : ActivityResultContract<Unit, WorldInfo<*>?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(context, CreateWorldActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): WorldInfo<*>? {
        return null
    }
}