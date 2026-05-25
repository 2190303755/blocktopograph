package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NotListedLocation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

fun spritePainter(
    sheet: ImageBitmap,
    uv: IntOffset,
    size: IntSize
): BitmapPainter {
    val maxWidth = sheet.width
    val maxHeight = sheet.height
    return BitmapPainter(
        image = sheet,
        srcOffset = IntOffset(
            uv.x.coerceAtMost(maxWidth),
            uv.y.coerceAtMost(maxHeight)
        ),
        srcSize = IntSize(
            size.width.coerceAtMost(maxWidth),
            size.height.coerceAtMost(maxHeight)
        ),
        filterQuality = DefaultFilterQuality
    )
}

@Composable
fun Sprite(
    sheet: ImageBitmap,
    uv: IntOffset,
    size: IntSize,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val painter = remember(sheet, uv, size) {
        spritePainter(sheet, uv, size)
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
    )
}

@Composable
fun Marker(
    sheet: ImageBitmap?,
    uv: IntOffset,
    size: IntSize,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholder: ImageVector = Icons.AutoMirrored.Filled.NotListedLocation,
) {
    if (sheet === null) {
        Icon(placeholder, contentDescription, modifier)
    } else {
        Sprite(sheet, uv, size, modifier, contentDescription)
    }
}