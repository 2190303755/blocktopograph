package com.mithrilmania.blocktopograph.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

object FilePicker : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?) = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").let {
            it.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input ?: return it)
        }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        if (resultCode == RESULT_OK) intent?.data else null
}