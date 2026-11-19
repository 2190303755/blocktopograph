package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Stable
fun PaddingValues.onlyTop() = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateBottomPadding(): Dp = 0.dp
    override fun calculateTopPadding(): Dp = this@onlyTop.calculateTopPadding()
}

@Stable
fun PaddingValues.excludeTop() = object : PaddingValues by this@excludeTop {
    override fun calculateTopPadding(): Dp = 0.dp
}

@Stable
fun PaddingValues.onlyHorizontal() = object : PaddingValues {
    override fun calculateTopPadding(): Dp = 0.dp
    override fun calculateBottomPadding(): Dp = 0.dp
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        this@onlyHorizontal.calculateLeftPadding(layoutDirection)

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        this@onlyHorizontal.calculateRightPadding(layoutDirection)
}

@Stable
fun PaddingValues.onlyBottom() = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateTopPadding(): Dp = 0.dp
    override fun calculateBottomPadding(): Dp = this@onlyBottom.calculateBottomPadding()
}

@Stable
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
