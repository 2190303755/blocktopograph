package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class HorizontalPadding(
    paddingValues: PaddingValues
) : PaddingValues by paddingValues {
    override fun calculateTopPadding(): Dp = 0.dp
    override fun calculateBottomPadding(): Dp = 0.dp
}
