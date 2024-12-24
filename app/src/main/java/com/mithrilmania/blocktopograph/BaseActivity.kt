package com.mithrilmania.blocktopograph

import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.View.OnApplyWindowInsetsListener
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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

    open fun updateDecorViewPadding(decorView: View, systemBars: Insets, ime: Insets) {
        decorView.updatePadding(
            top = 0,
            bottom = ime.bottom,
            left = systemBars.left,
            right = systemBars.right
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (this.supportDynamicColors) {
            DynamicColors.applyToActivityIfAvailable(this, DynamicColorsOptions.Builder().build())
        }
        window.let {
            it.decorView.setOnApplyWindowInsetsListener(this)
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }
    }

    final override fun onApplyWindowInsets(view: View, insets: WindowInsets): WindowInsets {
        isIndicatorEnabled =
            when (Settings.Secure.getInt(this.contentResolver, "navigation_mode", 0)) {
                INDICATOR_NAV_MODE_ANDROID, INDICATOR_NAV_MODE_HARMONY -> true
                else -> false
            }
        val compact = WindowInsetsCompat.toWindowInsetsCompat(insets)
        updateDecorViewPadding(
            view,
            compact.getInsets(WindowInsetsCompat.Type.systemBars()),
            compact.getInsets(WindowInsetsCompat.Type.ime())
        )
        return insets
    }
}