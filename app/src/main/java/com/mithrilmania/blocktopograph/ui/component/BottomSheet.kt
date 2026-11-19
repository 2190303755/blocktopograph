package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrDefault

val DragHandleVerticalPadding = 22.dp
val DockedDragHandleHeight = 4.dp
val DockedDragHandleWidth = 32.dp
val DragHandleConsumedHeight = DragHandleVerticalPadding * 2 + DockedDragHandleHeight

@OptIn(ExperimentalMaterial3Api::class)
@JvmField
val HiddenOrExpanded: Set<SheetValue> = hashSetOf(
    SheetValue.Hidden,
    SheetValue.Expanded
)

@OptIn(ExperimentalMaterial3Api::class)
@JvmField
val PartiallyOrFullyExpanded: Set<SheetValue> = hashSetOf(
    SheetValue.PartiallyExpanded,
    SheetValue.Expanded
)

@OptIn(ExperimentalMaterial3Api::class)
@JvmField
val AllSheetValues: Set<SheetValue> = hashSetOf(
    SheetValue.Hidden,
    SheetValue.PartiallyExpanded,
    SheetValue.Expanded
)

class DynamicBottomPaddingValues : PaddingValues {
    var bottom: Dp = 0.dp
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateTopPadding(): Dp = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateBottomPadding(): Dp = this.bottom
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingActionScaffold(
    state: SheetState,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val padding = remember { DynamicBottomPaddingValues() }
    Layout(
        contents = listOf(action, { content(padding) }),
        modifier = modifier
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val actionPlaceables = measurables[0].fastMap { it.measure(looseConstraints) }
        val actionHeight = actionPlaceables.fastMaxOfOrDefault(0) { it.height }
        padding.bottom = actionHeight.toDp()
        val contentPlaceables = measurables[1].fastMap { it.measure(looseConstraints) }
        val layoutHeight = contentPlaceables.fastMaxOfOrDefault(0) { it.height }
            .coerceAtLeast(constraints.minHeight)
        layout(
            contentPlaceables.fastMaxOfOrDefault(0) { it.width },
            layoutHeight
        ) {
            contentPlaceables.fastForEach { it.placeRelative(0, 0) }
            val actionY = (constraints.maxHeight - state.requireOffset().toInt())
                .fastCoerceIn(actionHeight, layoutHeight) - actionHeight
            actionPlaceables.fastForEach { it.placeRelative(0, actionY) }
        }
    }
}

