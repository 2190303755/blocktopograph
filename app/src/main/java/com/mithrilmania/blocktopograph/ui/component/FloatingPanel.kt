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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrDefault
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
            consumedVelocity = flingBehavior.run { scrollScope.performFling(initialVelocity) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Panel(
    state: FloatingPanelScaffoldState,
    floatingState: State<Boolean>,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val anchoredDraggableFlingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state.anchoredDraggableState,
        positionalThreshold = { density.run { 56.dp.toPx() } },
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
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(connection)
            .anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = Orientation.Vertical
            ),
        shape = if (floatingState.value) MaterialTheme.shapes.large else BottomSheetDefaults.ExpandedShape,
        color = BottomSheetDefaults.ContainerColor,
        shadowElevation = BottomSheetDefaults.Elevation,
        content = content
    )
}

/**
 * @see androidx.compose.material3.Scaffold
 * @see androidx.compose.material3.BottomSheetScaffold
 */
@Composable
fun FloatingPanelScaffold(
    scaffoldState: FloatingPanelScaffoldState,
    panelContent: @Composable (PaddingValues) -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    insets: WindowInsets = safeLayoutInsets(),
    floatingThreshold: Dp = 600.dp,
    floatingMaxWidth: Dp = 376.dp,
    action: @Composable () -> Unit = {},
    isPanelAction: State<Boolean>? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { outer ->
        SnackbarHost(outer) { inner -> SwipeableSnackbar(inner) }
    },
    content: @Composable () -> Unit
) {
    val panelPadding = remember { DynamicBottomPaddingValues() }
    val showMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val hideMotion = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val spatialFlingSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    SideEffect {
        scaffoldState.showMotionSpec = showMotion
        scaffoldState.hideMotionSpec = hideMotion
        scaffoldState.anchoredDraggableMotionSpec = spatialFlingSpec
    }
    FloatingPanelScaffoldLayout(
        panelPadding = panelPadding,
        modifier = modifier,
        state = scaffoldState,
        threshold = floatingThreshold,
        maxWidth = floatingMaxWidth,
        padding = padding,
        insets = insets,
        panel = {
            val floatingState = rememberSaveable { mutableStateOf(false) }
            SubcomposeLayout { constraints ->
                val layoutHeight = constraints.maxHeight
                val layoutWidth = constraints.maxWidth
                val isFloating = layoutWidth >= floatingThreshold.roundToPx()
                scaffoldState.updateAnchorsIfNeeded(this, layoutHeight, isFloating)
                val visibleHeight = layoutHeight - scaffoldState.offset.toInt()
                if (visibleHeight <= 0) return@SubcomposeLayout layout(
                    constraints.minWidth,
                    constraints.minWidth
                ) {}
                floatingState.value = isFloating
                val contentConstraints = if (isFloating) constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = (layoutWidth / 2).fastCoerceAtMost(floatingMaxWidth.roundToPx()),
                    maxHeight = visibleHeight
                ) else constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = subcompose(Unit) {
                    Panel(scaffoldState, floatingState) {
                        panelContent(panelPadding)
                    }
                }.fastMap { it.measure(contentConstraints) }

                layout(
                    placeables.fastMaxOfOrDefault(constraints.minWidth) { it.width },
                    placeables.fastMaxOfOrDefault(constraints.minHeight) { it.height }
                ) {
                    placeables.fastForEach { it.placeRelative(0, scaffoldState.offset.toInt()) }
                }
            }
        },
        action = action,
        isPanelAction = isPanelAction,
        snackbar = {
            snackbarHost(scaffoldState.snackbarHostState)
        },
        content = content
    )
}

@Composable
private fun FloatingPanelScaffoldLayout(
    panelPadding: DynamicBottomPaddingValues,
    modifier: Modifier,
    state: FloatingPanelScaffoldState,
    threshold: Dp,
    maxWidth: Dp,
    padding: Dp,
    insets: WindowInsets,
    panel: @Composable () -> Unit,
    content: @Composable () -> Unit,
    action: @Composable () -> Unit,
    isPanelAction: State<Boolean>?,
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

        val hasPanelAction = isPanelAction?.value ?: false
        val panelActionConstraints = panelConstraints.copy(
            maxWidth = if (isFloating) {
                val estimatedPanelWidth = (layoutWidth / 2).fastCoerceAtMost(maxWidth.roundToPx())
                estimatedPanelWidth.fastCoerceAtMost(
                    layoutWidth - estimatedPanelWidth - paddingLeft - paddingPx - paddingRight
                )
            } else {
                layoutWidth - paddingLeft - paddingRight
            }
        )
        val actionPlaceables = measurables[2].fastMap { it.measure(panelActionConstraints) }
        val actionWidth = actionPlaceables.fastMaxOfOrDefault(0) { it.width }
        panelPadding.bottom = if (isFloating || !hasPanelAction) 0.dp else {
            (actionPlaceables.fastMaxOfOrDefault(0) { it.height } + paddingPx + insetsBottom).toDp()
        }

        val panelPlaceables = measurables[1].fastMap { it.measure(panelConstraints) }
        val panelWidth = panelPlaceables.fastMaxOfOrDefault(0) { it.width }
        val effectivePanelWidth =
            panelWidth + if (state.reverseLayout) paddingRight else paddingLeft
        val workspaceWidth = layoutWidth - effectivePanelWidth

        val effectiveActionWidth: Int
        val snackbarPlaceables: List<Placeable>
        if (isFloating) {
            val workspaceConstraints = panelConstraints.copy(maxWidth = workspaceWidth)
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
            effectiveActionWidth = actionWidth
            snackbarPlaceables = measurables[3].fastMap { it.measure(looseConstraints) }
        }

        layout(layoutWidth, layoutHeight) {
            val actionHeight = actionPlaceables.fastMaxOfOrDefault(0) { it.height }

            val snackbarWidth = snackbarPlaceables.fastMaxOfOrDefault(0) { it.width }
            val snackbarHeight = snackbarPlaceables.fastMaxOfOrDefault(0) { it.height }

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
                            if (hasPanelAction) {
                                actionOffsetY =
                                    layoutHeight - actionHeight - paddingPx - insetsBottom
                                snackbarOffsetY = actionOffsetY - snackbarHeight
                            } else {
                                actionOffsetY = center - actionHeight - paddingPx
                                snackbarOffsetY = layoutHeight - snackbarHeight - insetsBottom
                            }
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
                if (hasPanelAction) {
                    panelPlaceables.fastForEach { it.placeRelative(0, 0) }

                    actionPlaceables.fastForEach { it.placeRelative(actionOffsetX, actionOffsetY) }
                } else {
                    actionPlaceables.fastForEach { it.placeRelative(actionOffsetX, actionOffsetY) }

                    panelPlaceables.fastForEach { it.placeRelative(0, 0) }
                }

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
