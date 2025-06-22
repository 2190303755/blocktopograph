package com.mithrilmania.blocktopograph.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_TITLE
import android.net.Uri
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import androidx.activity.result.contract.ActivityResultContract
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.util.FileCreator.Options

object FileCreator : ActivityResultContract<Options?, Uri?>() {
    override fun createIntent(context: Context, input: Options?): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        return input?.applyTo(intent) ?: intent.setType(MIME_TYPE_DEFAULT)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        if (resultCode == RESULT_OK) intent?.data else null

    data class Options(val mime: String, val location: Uri? = null, val name: String = "") {
        fun applyTo(intent: Intent): Intent {
            return intent.setType(this.mime)
                .putExtra(EXTRA_TITLE, this.name)
                .putExtra(EXTRA_INITIAL_URI, this.location ?: return intent)
        }
    }
}