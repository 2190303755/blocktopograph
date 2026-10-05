package com.mithrilmania.blocktopograph.ui.component

import androidx.annotation.Px
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.lerp


enum class PanelValue {
    Hidden,
    Collapse,
    Peek,
    Expanded
}

class FloatingPanelScaffoldState(
    initialValue: PanelValue,
    @JvmField val snackbarHostState: SnackbarHostState,
    @JvmField var allowHiddenState: Boolean = true,
    @JvmField val collapseOffset: (Density, Int, Boolean) -> Float
) {
    private var isFloating = false
    private var lastLayoutHeight = -1
    val anchoredDraggableState: AnchoredDraggableState<PanelValue> =
        AnchoredDraggableState(initialValue = initialValue)
    var showMotionSpec: FiniteAnimationSpec<Float> = snap()
    var hideMotionSpec: FiniteAnimationSpec<Float> = snap()
    var anchoredDraggableMotionSpec: AnimationSpec<Float> =
        tween(durationMillis = 300, easing = FastOutSlowInEasing)

    var reverseLayout by mutableStateOf(false)

    val offset: Float
        get() = anchoredDraggableState.offset
    val currentValue: PanelValue
        get() = anchoredDraggableState.settledValue
    val targetValue: PanelValue
        get() = anchoredDraggableState.targetValue

    suspend fun expand() {
        anchoredDraggableState.animateTo(PanelValue.Expanded, showMotionSpec)
    }

    suspend fun peek() {
        anchoredDraggableState.animateTo(PanelValue.Peek, showMotionSpec)
    }

    suspend fun collapse() {
        anchoredDraggableState.animateTo(PanelValue.Collapse, hideMotionSpec)
    }

    suspend fun hide() {
        anchoredDraggableState.animateTo(PanelValue.Hidden, hideMotionSpec)
    }

    suspend fun anchoredDrag(flingBehavior: FlingBehavior, initialVelocity: Float): Float {
        var consumedVelocity = 0f
        anchoredDraggableState.anchoredDrag {
            val scrollScope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    val newOffset = ((if (offset.isNaN()) 0.0F else offset) + pixels).coerceIn(
                        anchoredDraggableState.anchors.minPosition(),
                        anchoredDraggableState.anchors.maxPosition(),
                    )
                    val consumed = newOffset - offset
                    dragTo(newOffset)
                    return consumed
                }
            }
            consumedVelocity = with(flingBehavior) { scrollScope.performFling(initialVelocity) }
        }
        return consumedVelocity
    }

    fun updateAnchorsIfNeeded(density: Density, @Px height: Int, isFloating: Boolean) {
        if (this.lastLayoutHeight != height || this.isFloating != isFloating) {
            this.updateAnchors(density, height, isFloating)
        }
    }

    fun updateAnchors(density: Density, @Px height: Int, isFloating: Boolean) {
        this.lastLayoutHeight = height
        this.isFloating = isFloating
        this.anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                if (allowHiddenState) {
                    PanelValue.Hidden at height.toFloat()
                }
                PanelValue.Collapse at collapseOffset(density, height, isFloating)
                PanelValue.Peek at height * 0.5F
                PanelValue.Expanded at 0F
            }
        )
    }
}

@Composable
fun rememberFloatingPanelScaffoldState(
    initialValue: PanelValue = PanelValue.Peek,
    allowHiddenState: Boolean = true,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    collapseOffset: (Density, Int, Boolean) -> Float,
): FloatingPanelScaffoldState = remember(
    initialValue,
    snackbarHostState,
    allowHiddenState,
    collapseOffset
) {
    FloatingPanelScaffoldState(
        initialValue = initialValue,
        snackbarHostState = snackbarHostState,
        allowHiddenState = allowHiddenState,
        collapseOffset = collapseOffset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Panel(
    state: FloatingPanelScaffoldState,
    isFloating: Boolean,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val anchoredDraggableFlingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state.anchoredDraggableState,
        positionalThreshold = { with(density) { 56.dp.toPx() } },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    )

    val connection = remember(state.anchoredDraggableState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                return if (delta < 0 && source == NestedScrollSource.UserInput) {
                    Offset(0.0F, state.anchoredDraggableState.dispatchRawDelta(delta))
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = available.y
                return if (source == NestedScrollSource.UserInput && delta != 0f) {
                    Offset(0.0F, state.anchoredDraggableState.dispatchRawDelta(delta))
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val toFling = available.y
                val currentOffset = state.offset
                val minAnchor = state.anchoredDraggableState.anchors.minPosition()
                return if (toFling < 0 && currentOffset > minAnchor) {
                    state.anchoredDrag(anchoredDraggableFlingBehavior, toFling)
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return Velocity(
                    consumed.x,
                    state.anchoredDrag(anchoredDraggableFlingBehavior, available.y)
                )
            }
        }
    }
    Surface(
        modifier = if (isFloating) {
            Modifier
                .fillMaxHeight()
                .widthIn(max = 376.dp)
        } else {
            Modifier.fillMaxSize()
        }
            .nestedScroll(connection)
            .anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = Orientation.Vertical
            ),
        shape = if (isFloating) MaterialTheme.shapes.large else BottomSheetDefaults.ExpandedShape,
        color = BottomSheetDefaults.ContainerColor,
        shadowElevation = BottomSheetDefaults.Elevation,
        content = content
    )
}

/**
 * @see Scaffold
 * @see BottomSheetScaffold
 */
@Composable
fun FloatingPanelScaffold(
    scaffoldState: FloatingPanelScaffoldState,
    panelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    insets: WindowInsets = safeLayoutInsets(),
    floatingThreshold: Dp = 600.dp,
    action: @Composable () -> Unit = { },
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    content: @Composable () -> Unit
) {
    val showMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val hideMotion = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val spatialFlingSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    SideEffect {
        scaffoldState.showMotionSpec = showMotion
        scaffoldState.hideMotionSpec = hideMotion
        scaffoldState.anchoredDraggableMotionSpec = spatialFlingSpec
    }
    FloatingPanelScaffoldLayout(
        modifier = modifier,
        state = scaffoldState,
        threshold = floatingThreshold,
        padding = padding,
        insets = insets,
        panel = {
            SubcomposeLayout { constraints ->
                val layoutWidth = constraints.maxWidth
                val layoutHeight = constraints.maxHeight
                val isFloating = layoutWidth >= floatingThreshold.roundToPx()
                scaffoldState.updateAnchorsIfNeeded(this, layoutHeight, isFloating)
                val contentConstraints = if (isFloating) constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = layoutWidth / 2,
                    maxHeight = (layoutHeight - scaffoldState.offset.toInt()).fastCoerceAtLeast(0)
                ) else constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = subcompose(Unit) {
                    Panel(scaffoldState, isFloating, panelContent)
                }.fastMap { it.measure(contentConstraints) }

                layout(
                    placeables.fastMaxOfOrNull { it.width } ?: constraints.minWidth,
                    placeables.fastMaxOfOrNull { it.height } ?: constraints.minHeight
                ) {
                    placeables.fastForEach { it.placeRelative(0, scaffoldState.offset.toInt()) }
                }
            }
        },
        action = action,
        snackbar = {
            snackbarHost(scaffoldState.snackbarHostState)
        },
        content = content
    )
}

@Composable
private fun FloatingPanelScaffoldLayout(
    modifier: Modifier,
    state: FloatingPanelScaffoldState,
    threshold: Dp,
    padding: Dp,
    insets: WindowInsets,
    panel: @Composable () -> Unit,
    content: @Composable () -> Unit,
    action: @Composable () -> Unit,
    snackbar: @Composable () -> Unit
) {
    Layout(
        contents = listOf(content, panel, action, snackbar),
        modifier = modifier.fillMaxSize()
    ) { measurables, constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val paddingPx = padding.roundToPx()
        val paddingLeft = insets.getLeft(this, this.layoutDirection).fastCoerceAtLeast(paddingPx)
        val paddingTop = insets.getTop(this).fastCoerceAtLeast(paddingPx)
        val paddingRight = insets.getRight(this, this.layoutDirection).fastCoerceAtLeast(paddingPx)
        val insetsBottom = insets.getBottom(this)
        val paddingBottom = insetsBottom.fastCoerceAtLeast(paddingPx)
        val isFloating = layoutWidth >= threshold.roundToPx()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val contentPlaceables = measurables[0].fastMap { it.measure(looseConstraints) }

        val panelConstraints = if (isFloating) {
            looseConstraints.copy(maxHeight = layoutHeight - paddingTop - paddingBottom)
        } else looseConstraints
        val panelPlaceables = measurables[1].fastMap { it.measure(panelConstraints) }
        val panelWidth = panelPlaceables.fastMaxOfOrNull { it.width } ?: 0
        val effectivePanelWidth =
            panelWidth + if (state.reverseLayout) paddingRight else paddingLeft
        val workspaceWidth = layoutWidth - effectivePanelWidth

        val actionWidth: Int
        val effectiveActionWidth: Int
        val actionPlaceables: List<Placeable>
        val snackbarPlaceables: List<Placeable>
        if (isFloating) {
            val workspaceConstraints = panelConstraints.copy(maxWidth = workspaceWidth)
            actionPlaceables = measurables[2].fastMap { it.measure(workspaceConstraints) }
            actionWidth = actionPlaceables.fastMaxOfOrNull { it.width } ?: 0
            effectiveActionWidth =
                actionWidth + if (state.reverseLayout) paddingLeft else paddingRight

            val snackbarConstraints = if (state.offset >= layoutHeight) {
                looseConstraints.copy(maxWidth = layoutWidth - effectiveActionWidth)
            } else if (workspaceWidth - effectiveActionWidth > effectivePanelWidth) {
                looseConstraints.copy(maxWidth = workspaceWidth - effectiveActionWidth)
            } else {
                workspaceConstraints
            }
            snackbarPlaceables = measurables[3].fastMap { it.measure(snackbarConstraints) }
        } else {
            actionPlaceables = measurables[2].fastMap { it.measure(looseConstraints) }
            actionWidth = actionPlaceables.fastMaxOfOrNull { it.width } ?: 0
            effectiveActionWidth = actionWidth
            snackbarPlaceables = measurables[3].fastMap { it.measure(looseConstraints) }
        }

        layout(layoutWidth, layoutHeight) {
            val actionHeight = actionPlaceables.fastMaxOfOrNull { it.height } ?: 0

            val snackbarWidth = snackbarPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val snackbarHeight = snackbarPlaceables.fastMaxOfOrNull { it.height } ?: 0

            contentPlaceables.fastForEach { it.placeRelative(0, 0) }
            if (isFloating) {
                val actionOffsetY = layoutHeight - actionHeight - paddingBottom
                if (state.reverseLayout) {
                    actionPlaceables.fastForEach { it.placeRelative(paddingLeft, actionOffsetY) }

                    panelPlaceables.fastForEach { it.placeRelative(workspaceWidth, paddingTop) }
                } else {
                    val actionOffsetX = layoutWidth - actionWidth - paddingRight
                    actionPlaceables.fastForEach { it.placeRelative(actionOffsetX, actionOffsetY) }

                    panelPlaceables.fastForEach { it.placeRelative(paddingLeft, paddingTop) }
                }

                val snackbarOffsetX: Int
                val snackbarOffsetY: Int
                if (state.offset >= layoutHeight) {
                    snackbarOffsetX = (layoutWidth - effectiveActionWidth - snackbarWidth) / 2 +
                            if (state.reverseLayout) effectiveActionWidth else 0
                    snackbarOffsetY = layoutHeight - snackbarHeight - insetsBottom
                } else if (workspaceWidth - effectiveActionWidth > effectivePanelWidth) {
                    snackbarOffsetX =
                        (workspaceWidth - effectiveActionWidth - snackbarWidth) / 2 +
                                if (state.reverseLayout) effectiveActionWidth else effectivePanelWidth
                    snackbarOffsetY = layoutHeight - snackbarHeight - insetsBottom
                } else {
                    snackbarOffsetX = (workspaceWidth - snackbarWidth) / 2 +
                            if (state.reverseLayout) 0 else effectivePanelWidth
                    snackbarOffsetY = layoutHeight - snackbarHeight - actionHeight - paddingBottom
                }
                snackbarPlaceables.fastForEach {
                    it.placeRelative(
                        snackbarOffsetX,
                        snackbarOffsetY
                    )
                }
            } else {
                val actionOffsetY: Int
                val snackbarOffsetY: Int

                when (state.targetValue) {
                    PanelValue.Expanded, PanelValue.Peek -> {
                        val center = layoutHeight / 2
                        if (state.offset < center) {
                            actionOffsetY = center - actionHeight - paddingPx
                            snackbarOffsetY = layoutHeight - snackbarHeight - insetsBottom
                        } else {
                            actionOffsetY = state.offset.toInt() - actionHeight - paddingPx
                            snackbarOffsetY = actionOffsetY - snackbarHeight
                        }
                    }

                    else -> {
                        actionOffsetY = state.offset.toInt() - actionHeight - lerp(
                            paddingPx,
                            paddingBottom,
                            state.anchoredDraggableState.progress(
                                PanelValue.Collapse,
                                PanelValue.Hidden
                            )
                        )
                        snackbarOffsetY = actionOffsetY - snackbarHeight
                    }
                }

                val actionOffsetX = layoutWidth - actionWidth - paddingRight
                actionPlaceables.fastForEach { it.placeRelative(actionOffsetX, actionOffsetY) }

                panelPlaceables.fastForEach { it.placeRelative(0, 0) }

                val snackbarOffsetX = (layoutWidth - snackbarWidth) / 2
                snackbarPlaceables.fastForEach {
                    it.placeRelative(
                        snackbarOffsetX,
                        snackbarOffsetY
                    )
                }
            }
        }
    }
}
