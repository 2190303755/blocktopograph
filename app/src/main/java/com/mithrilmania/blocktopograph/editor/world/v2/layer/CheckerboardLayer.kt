package com.mithrilmania.blocktopograph.editor.world.v2.layer

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

val BACKGROUND_SHADER = BitmapShader(
    makeCheckerboard(0xFF2B2B2B.toInt(), 0xFF585858.toInt()),
    Shader.TileMode.REPEAT,
    Shader.TileMode.REPEAT
)
val ERROR_SHADER = BitmapShader(
    makeCheckerboard(0xFF2B0000.toInt(), 0xFF580000.toInt()),
    Shader.TileMode.REPEAT,
    Shader.TileMode.REPEAT
)

fun makeCheckerboard(dark: Int, light: Int): Bitmap {
    val bitmap = createBitmap(2, 2, Bitmap.Config.RGB_565)
    bitmap[0, 0] = dark
    bitmap[0, 1] = light
    bitmap[1, 0] = light
    bitmap[1, 1] = dark
    return bitmap
}
