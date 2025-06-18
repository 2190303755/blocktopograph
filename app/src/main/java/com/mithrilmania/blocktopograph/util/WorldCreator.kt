package com.mithrilmania.blocktopograph.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.mithrilmania.blocktopograph.editor.world.CreateWorldActivity

object WorldCreator : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(context, CreateWorldActivity::class.java)

    override fun parseResult(code: Int, intent: Intent?): Uri? {
        return if (code == RESULT_OK) intent?.data else null
    }
}