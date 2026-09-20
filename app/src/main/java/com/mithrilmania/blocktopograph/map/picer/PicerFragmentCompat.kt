package com.mithrilmania.blocktopograph.map.picer

import android.graphics.Bitmap
import android.widget.ScrollView
import coil3.load
import com.mithrilmania.blocktopograph.databinding.FragPicerBinding

fun loadBitmap(binding: FragPicerBinding, bitmap: Bitmap) {
    binding.image.load(bitmap) {
        listener(
            onSuccess = { _, _ ->
                binding.scroll.post {
                    binding.scroll.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        )
    }
}
