package com.mithrilmania.blocktopograph.util

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomsheet.BottomSheetBehavior.SAVE_SKIP_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

const val INDICATOR_NAV_MODE_ANDROID = 2
const val INDICATOR_NAV_MODE_HARMONY = 105
const val VIEW_DOCUMENT_FLAG =
    Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

fun Context.upcoming() {
    Toast.makeText(this, "前面的区域，以后再来探索吧！", Toast.LENGTH_SHORT).show()
}

fun Context.toast(@StringRes text: Int) =
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

val Context.clipboard
    get() = this.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

inline fun FragmentManager.popAndTransit(action: FragmentTransaction.() -> Unit) {
    this.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.beginTransaction().apply { action() }.commit()
}

fun BottomSheetDialogFragment.makeCommonDialog() =
    BottomSheetDialog(this.requireContext(), this.theme).apply {
        dismissWithAnimation = true
        behavior.apply {
            skipCollapsed = true
            saveFlags = SAVE_SKIP_COLLAPSED
            state = STATE_EXPANDED
        }
    }

inline fun <T : DialogFragment> FragmentActivity.showIfAbsent(tag: String, factory: () -> T) {
    this.supportFragmentManager.apply {
        if (this.findFragmentByTag(tag) === null) {
            factory().show(this, tag)
        }
    }
}