package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

inline fun Int.spacedBy(space: () -> Int): Int = if (0 == this) 0 else this + space()

val EmptySpacer: @Composable () -> Unit = {
    Spacer(Modifier.size(Dp.Hairline))
}