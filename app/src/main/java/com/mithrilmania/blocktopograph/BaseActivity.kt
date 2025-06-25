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
import androidx.core.view.WindowInsetsCompat.Type
import com.mithrilmania.blocktopograph.util.INDICATOR_NAV_MODE_ANDROID
import com.mithrilmania.blocktopograph.util.INDICATOR_NAV_MODE_HARMONY

abstract class BaseActivity : AppCompatActivity(), OnApplyWindowInsetsListener {
    var isIndicatorEnabled = false
        private set

    abstract fun applyContentInsets(window: View, insets: Insets)

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
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
}