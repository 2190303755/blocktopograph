package com.mithrilmania.blocktopograph.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.mithrilmania.blocktopograph.BiomeSelectDialog
import com.mithrilmania.blocktopograph.map.Biome

object BiomePicker : ActivityResultContract<Any?, Biome?>() {
    override fun createIntent(context: Context, input: Any?): Intent {
        return Intent(context, BiomeSelectDialog::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        if (resultCode == RESULT_OK) intent?.getTypedSerializableExtra<Biome>(BiomeSelectDialog.KEY_BIOME) else null
}