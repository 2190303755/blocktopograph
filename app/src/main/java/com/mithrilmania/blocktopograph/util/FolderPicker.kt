package com.mithrilmania.blocktopograph.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

object FolderPicker : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        return intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input ?: return intent)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        if (resultCode == RESULT_OK) intent?.data else null
}