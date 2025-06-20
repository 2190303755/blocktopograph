package com.mithrilmania.blocktopograph

import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.View.OnApplyWindowInsetsListener
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.mithrilmania.blocktopograph.util.INDICATOR_NAV_MODE_ANDROID
import com.mithrilmania.blocktopograph.util.INDICATOR_NAV_MODE_HARMONY

abstract class BaseActivity(
    val supportDynamicColors: Boolean = true,
) : AppCompatActivity(), OnApplyWindowInsetsListener {
    var isIndicatorEnabled = false
        private set

    abstract fun applyContentInsets(window: View, insets: Insets)

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        if (this.supportDynamicColors) {
            DynamicColors.applyToActivityIfAvailable(this, DynamicColorsOptions.Builder().build())
        }
        this.window.let {
            it.decorView.setOnApplyWindowInsetsListener(this)
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }
    }

    final override fun onApplyWindowInsets(view: View, insets: WindowInsets): WindowInsets {
        isIndicatorEnabled = when (
            Settings.Secure.getInt(this.contentResolver, "navigation_mode", 0)
        ) {
            INDICATOR_NAV_MODE_ANDROID, INDICATOR_NAV_MODE_HARMONY -> true
            else -> false
        }
        this.applyContentInsets(
            view,
            WindowInsetsCompat.toWindowInsetsCompat(insets).getInsets(
                Type.systemBars() or Type.displayCutout() or Type.ime()
            )
        )
        return insets
    }

    companion object {
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

        inline fun View.applyFloatingInsets(insets: Insets, base: () -> Int) {
            val padding = base()
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + padding
                leftMargin = insets.left + padding
                rightMargin = insets.right + padding
            }
        }
    }
}