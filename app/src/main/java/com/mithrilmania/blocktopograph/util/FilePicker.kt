package com.mithrilmania.blocktopograph.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

object FilePicker : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, initial: Uri?) = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").let {
            it.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial ?: return it)
        }

    override fun parseResult(code: Int, intent: Intent?) = if (code == RESULT_OK) intent?.data else null
}