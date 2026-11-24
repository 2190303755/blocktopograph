package ovh.plrapps.mapcompose.ui.layout

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.gestures.detectTapGestures
import ovh.plrapps.mapcompose.ui.gestures.detectTransformGestures
import ovh.plrapps.mapcompose.ui.state.ZoomPanState

@Composable
internal fun ZoomPan(
    modifier: Modifier = Modifier,
    zoomPanState: ZoomPanState,
    visibleAreaPadding: (Density, IntSize) -> IntOffset,
    onTap: (x: Double, y: Double) -> Unit,
    onLongPress: (x: Double, y: Double) -> Unit,
    onTouchDown: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val flingSpec = rememberSplineBasedDecay<Offset>()

    Layout(
        content = content,
        modifier = modifier
            .pointerInput(zoomPanState.isListeningForGestures()) {
                if (!zoomPanState.isListeningForGestures()) return@pointerInput
                detectTransformGestures(
                    onGesture = { pivot, pan, gestureZoom ->
                        zoomPanState.onScaleRatio(gestureZoom.toDouble(), pivot)
                        zoomPanState.onScrollDelta(pan)
                    },
                    onTouchDown = {
                        onTouchDown()
                        scope.launch { zoomPanState.onTouchDown() }
                    },
                    onTwoFingersTap = { pivot ->
                        scope.launch { zoomPanState.onTwoFingersTap(pivot) }
                    },
                    onFling = { velocity ->
                        scope.launch { zoomPanState.onFling(flingSpec, velocity) }
                    },
                    onFlingZoom = { velocity, pivot ->
                        scope.launch { zoomPanState.onFlingZoom(velocity, pivot) }
                    }
                )
            }
            .pointerInput(zoomPanState.isListeningForGestures()) {
                if (!zoomPanState.isListeningForGestures()) return@pointerInput
                detectTapGestures(
                    onTap = { offset ->
                        onTap(zoomPanState.absoluteX(offset), zoomPanState.absoluteY(offset))
                    },
                    onDoubleTap = { offset ->
                        scope.launch { zoomPanState.onDoubleTap(offset) }
                    },
                    onDoubleTapZoom = { pivot, zoom ->
                        zoomPanState.onScaleRatio(zoom.toDouble(), pivot)
                    },
                    onDoubleTapZoomFling = { velocity, pivot ->
                        scope.launch { zoomPanState.onFlingZoom(velocity, pivot) }
                    },
                    onLongPress = { offset ->
                        onLongPress(zoomPanState.absoluteX(offset), zoomPanState.absoluteY(offset))
                    }
                )
            }
            .fillMaxSize(),
    ) { measurables, constraints ->
        /* The MeasureScope is a Density, so the padding lambda accesses the current environment. */
        val size = IntSize(constraints.maxWidth, constraints.maxHeight)
        zoomPanState.onMeasure(size, visibleAreaPadding(this, size))


        val placeables = measurables.fastMap { measurable ->
            // Measure each child
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Place children in the parent layout
            placeables.fastForEach { placeable ->
                placeable.place(x = 0, y = 0)
            }
        }
    }
}
