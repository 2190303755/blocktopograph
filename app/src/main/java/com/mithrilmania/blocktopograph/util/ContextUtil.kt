package com.mithrilmania.blocktopograph.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

const val VIEW_DOCUMENT_FLAG =
    Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

fun Context.upcoming() {
    Toast.makeText(this, "前面的区域，以后再来探索吧！", Toast.LENGTH_SHORT).show()
}

fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

inline fun FragmentManager.popAndTransit(action: FragmentTransaction.() -> Unit) {
    this.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.beginTransaction().apply { action() }.commit()
}

