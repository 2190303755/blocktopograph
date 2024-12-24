package com.mithrilmania.blocktopograph.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

object FolderPicker : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, initial: Uri?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        return intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial ?: return intent)
    }

    override fun parseResult(code: Int, intent: Intent?) =
        if (code == RESULT_OK) intent?.data else null
}