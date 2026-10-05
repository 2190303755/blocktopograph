package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.ui.unit.dp

val DragHandleVerticalPadding = 22.dp
val DockedDragHandleHeight = 4.dp
val DockedDragHandleWidth = 32.dp
val DragHandleConsumedHeight = DragHandleVerticalPadding * 2 + DockedDragHandleHeight

@OptIn(ExperimentalMaterial3Api::class)
val HiddenOrExpanded: Set<SheetValue> = hashSetOf(
    SheetValue.Hidden,
    SheetValue.Expanded
)

@OptIn(ExperimentalMaterial3Api::class)
val PartiallyOrFullyExpanded: Set<SheetValue> = hashSetOf(
    SheetValue.PartiallyExpanded,
    SheetValue.Expanded
)

@OptIn(ExperimentalMaterial3Api::class)
val AllSheetValues: Set<SheetValue> = hashSetOf(
    SheetValue.Hidden,
    SheetValue.PartiallyExpanded,
    SheetValue.Expanded
)
