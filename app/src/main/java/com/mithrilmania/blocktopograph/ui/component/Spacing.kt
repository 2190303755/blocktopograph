package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class HorizontalPadding(
    paddingValues: PaddingValues
) : PaddingValues by paddingValues {
    override fun calculateTopPadding(): Dp = 0.dp
    override fun calculateBottomPadding(): Dp = 0.dp
}

fun WindowInsets(
    horizontal: Dp,
    vertical: Dp
) = WindowInsets(
    left = horizontal,
    top = vertical,
    right = horizontal,
    bottom = vertical
)

@Composable
fun safeLayoutInsets(): WindowInsets =
    WindowInsets.systemBars.union(WindowInsets.displayCutout)
