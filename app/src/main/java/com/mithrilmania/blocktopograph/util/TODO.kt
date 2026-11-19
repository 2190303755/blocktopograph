package com.mithrilmania.blocktopograph.util

import android.content.Context
import android.widget.Toast

fun String.todoI18n() = this

fun Context.upcoming() {
    Toast.makeText(this, "前面的区域，以后再来探索吧！", Toast.LENGTH_SHORT).show()
}
