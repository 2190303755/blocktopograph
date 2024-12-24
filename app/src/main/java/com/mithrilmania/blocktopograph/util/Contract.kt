package com.mithrilmania.blocktopograph.util

import android.content.Context
import android.content.Intent
import android.widget.Toast

const val INDICATOR_NAV_MODE_ANDROID = 2
const val INDICATOR_NAV_MODE_HARMONY = 105
const val VIEW_DOCUMENT_FLAG =
    Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

fun Context.upcoming() {
    Toast.makeText(this, "前面的区域，以后再来探索吧！", Toast.LENGTH_SHORT).show()
}
