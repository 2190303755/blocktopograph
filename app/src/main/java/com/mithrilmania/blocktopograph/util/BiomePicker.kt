package com.mithrilmania.blocktopograph.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.mithrilmania.blocktopograph.BiomeSelectDialog
import com.mithrilmania.blocktopograph.map.Biome

object BiomePicker : ActivityResultContract<Any?, Biome?>() {
    override fun createIntent(context: Context, ignored: Any?): Intent {
        return Intent(context, BiomeSelectDialog::class.java)
    }

    override fun parseResult(code: Int, intent: Intent?) =
        if (code == RESULT_OK) intent?.getSerializableExtra(BiomeSelectDialog.KEY_BIOME) as? Biome else null
}