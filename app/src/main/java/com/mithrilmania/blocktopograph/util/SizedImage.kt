package com.mithrilmania.blocktopograph.util

import androidx.compose.ui.graphics.FilterQuality
import coil3.size.Size
import coil3.size.SizeResolver

data class SizedImage(
    @JvmField val data: Any,
    @JvmField val size: Size,
    val quality: FilterQuality
) : SizeResolver {
    override suspend fun size(): Size = size
}
