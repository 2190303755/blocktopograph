package com.mithrilmania.blocktopograph.map.selection

import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.mithrilmania.blocktopograph.R
import java.lang.ref.WeakReference

abstract class MeowWatcherCompat(
    val which: WeakReference<EditText?>
) : TextWatcher {
    abstract var selection: Rect
    override fun afterTextChanged(editable: Editable) {
        val which = this.which.get() ?: return
        val value = editable.toString().toIntOrNull() ?: return
        val mSelection = this.selection
        when (which.id) {
            R.id.from_x_text -> {
                if (mSelection.left == value) return
                which.removeTextChangedListener(this)
                mSelection.right += value - mSelection.left
                mSelection.left = value
            }

            R.id.from_y_text -> {
                if (mSelection.top == value) return
                which.removeTextChangedListener(this)
                mSelection.bottom += value - mSelection.top
                mSelection.top = value
            }

            R.id.range_w_text -> {
                if (mSelection.right - mSelection.left == value) return
                which.removeTextChangedListener(this)
                mSelection.right = mSelection.left + value
            }

            R.id.range_h_text -> {
                if (mSelection.bottom - mSelection.top == value) return
                which.removeTextChangedListener(this)
                mSelection.bottom = mSelection.top + value
            }
        }
        this.selection = mSelection
        which.addTextChangedListener(this)
    }
}