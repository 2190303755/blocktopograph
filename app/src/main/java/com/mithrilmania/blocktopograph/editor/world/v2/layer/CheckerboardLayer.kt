package com.mithrilmania.blocktopograph.editor.world.v2.layer

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

val BACKGROUND_PATTERN = makeCheckerboard(0xFF2B2B2B.toInt(), 0xFF585858.toInt())
val ERROR_PATTERN = makeCheckerboard(0xFF2B0000.toInt(), 0xFF580000.toInt())

fun makeCheckerboard(dark: Int, light: Int): Bitmap {
    val bitmap = createBitmap(16, 16, Bitmap.Config.RGB_565)
    repeat(16) { y ->
        repeat(16) { x ->
            bitmap[x, y] = if ((x xor y and 1) == 0) dark else light
        }
    }
    return bitmap
}
