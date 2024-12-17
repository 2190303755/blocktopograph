package com.mithrilmania.blocktopograph.util

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors

internal val DefaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
internal val DefaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)


@Suppress("DEPRECATION")
fun Window.updateColor(usingIndicator: Boolean) {
    val view = this.decorView
    val isDark =
        (view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    WindowInsetsControllerCompat(this, view).run {
        isAppearanceLightStatusBars = !isDark
        isAppearanceLightNavigationBars = !isDark
    }
    this.statusBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        this.navigationBarColor = if (isDark) DefaultLightScrim else DefaultDarkScrim
    } else {
        this.navigationBarColor =
            if (usingIndicator) Color.TRANSPARENT else MaterialColors.getColor(
                this.context,
                android.R.attr.navigationBarColor,
                Color.BLACK
            )
        this.isStatusBarContrastEnforced = false
        this.isNavigationBarContrastEnforced = true
    }
}