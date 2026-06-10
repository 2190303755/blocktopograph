package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.BottomSheetState
import com.composeunstyled.DragIndication
import com.composeunstyled.Sheet
import com.composeunstyled.UnstyledBottomSheet

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    sheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    offsetForIme: Boolean = false,
    sheetPeekHeight: Dp = BottomSheetDefaults.SheetPeekHeight,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetShape: Shape = BottomSheetDefaults.ExpandedShape,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(sheetContainerColor),
    sheetTonalElevation: Dp = 0.dp,
    sheetShadowElevation: Dp = BottomSheetDefaults.Elevation,
    content: @Composable () -> Unit,
) {
    UnstyledBottomSheet(
        state = sheetState,
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned {
                sheetState.invalidateDetents()
            },
        offsetForIme = offsetForIme
    ) {
        Sheet(
            modifier = Modifier
                .requiredHeightIn(min = sheetPeekHeight)
                .widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
        ) {
            Surface(
                shape = sheetShape,
                color = sheetContainerColor,
                contentColor = sheetContentColor,
                tonalElevation = sheetTonalElevation,
                shadowElevation = sheetShadowElevation
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DragIndication(
                        modifier = Modifier
                            .padding(vertical = DragHandleVerticalPadding)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                MaterialTheme.shapes.extraLarge
                            )
                            .size(DockedDragHandleWidth, DockedDragHandleHeight),
                        indication = LocalIndication.current,
                    )
                    content()
                }
            }
        }
    }
}

