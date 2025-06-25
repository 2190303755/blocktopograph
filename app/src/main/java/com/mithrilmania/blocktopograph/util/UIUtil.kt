package com.mithrilmania.blocktopograph.util

import android.content.ClipData
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.textview.MaterialTextView
import com.mithrilmania.blocktopograph.R


// mark as reified to enable inlining
inline fun <reified T : ViewGroup> T.applyListInsets(
    isIndicatorEnabled: Boolean,
    insets: Insets,
    bottom: Int,
    horizontal: Int = 0,
) {
    if (isIndicatorEnabled) {
        updatePadding(
            bottom = bottom + insets.bottom,
            left = insets.left + horizontal,
            right = insets.right + horizontal
        )
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = 0
        }
    } else {
        updatePadding(
            bottom = bottom,
            left = insets.left + horizontal,
            right = insets.right + horizontal
        )
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = insets.bottom
        }
    }
}

fun View.applyFloatingInsets(insets: Insets, padding: Int) {
    updateLayoutParams<ViewGroup.MarginLayoutParams> {
        bottomMargin = insets.bottom + padding
        leftMargin = insets.left + padding
        rightMargin = insets.right + padding
    }
}

fun MaterialTextView.copyOnClick() {
    (this.parent?.parent?.parent as? View)?.setOnClickListener {
        if (this.text.isNullOrBlank()) return@setOnClickListener
        this.context.clipboard?.setPrimaryClip(ClipData.newPlainText("", this.text))
            ?: return@setOnClickListener
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            this.context.toast(R.string.toast_copy_success)
        }
    }
}