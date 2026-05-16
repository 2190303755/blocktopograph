package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

inline fun Int.spacedBy(space: () -> Int): Int = if (0 == this) 0 else this + space()

class HorizontalPadding(
    paddingValues: PaddingValues
) : PaddingValues by paddingValues {
    override fun calculateTopPadding(): Dp = 0.dp
    override fun calculateBottomPadding(): Dp = 0.dp
}

val EmptySpacer: @Composable () -> Unit = {
    Spacer(Modifier.size(Dp.Hairline))
}