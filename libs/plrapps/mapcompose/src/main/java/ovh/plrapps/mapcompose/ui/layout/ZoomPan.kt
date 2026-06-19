package ovh.plrapps.mapcompose.ui.layout

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.CoroutineScope
import ovh.plrapps.mapcompose.ui.gestures.detectTapGestures
import ovh.plrapps.mapcompose.ui.gestures.detectTransformGestures

@Composable
internal fun ZoomPan(
    modifier: Modifier = Modifier,
    gestureListener: GestureListener,
    layoutSizeChangeListener: LayoutSizeChangeListener,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val flingSpec = rememberSplineBasedDecay<Offset>()

    Layout(
        content = content,
        modifier
            .pointerInput(gestureListener.isListeningForGestures()) {
                if (!gestureListener.isListeningForGestures()) return@pointerInput
                detectTransformGestures(
                    onGesture = { pivot, pan, gestureZoom ->
                        gestureListener.onScaleRatio(gestureZoom.toDouble(), pivot)
                        gestureListener.onScrollDelta(pan)
                    },
                    onTouchDown = gestureListener::onTouchDown,
                    onTwoFingersTap = gestureListener::onTwoFingersTap,
                    onFling = { velocity -> gestureListener.onFling(flingSpec, velocity) },
                    onFlingZoom = gestureListener::onFlingZoom
                )
            }
            .pointerInput(gestureListener.isListeningForGestures()) {
                if (!gestureListener.isListeningForGestures()) return@pointerInput
                detectTapGestures(
                    onTap = gestureListener::onTap,
                    onDoubleTap = gestureListener::onDoubleTap,
                    onDoubleTapZoom = { pivot, zoom ->
                        gestureListener.onScaleRatio(zoom.toDouble(), pivot)
                    },
                    onDoubleTapZoomFling = gestureListener::onFlingZoom,
                    onPress = { gestureListener.onPress() },
                    onLongPress = gestureListener::onLongPress,
                    shouldConsumeTap = gestureListener::shouldConsumeTapGesture,
                    shouldConsumeLongPress = gestureListener::shouldConsumeLongPress
                )
            }
            .onSizeChanged {
                layoutSizeChangeListener.onSizeChanged(scope, it)
            }
            .fillMaxSize(),
    ) { measurables, constraints ->
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

internal interface GestureListener {
    fun onScaleRatio(scaleRatio: Double, pivot: Offset)
    fun onScrollDelta(scrollDelta: Offset)
    fun onFling(flingSpec: DecayAnimationSpec<Offset>, velocity: Velocity)
    fun onFlingZoom(velocity: Float, pivot: Offset)
    fun onTouchDown()
    fun onPress()
    fun onTap(focalPt: Offset)
    fun onDoubleTap(focalPt: Offset)
    fun onTwoFingersTap(focalPt: Offset)
    fun onLongPress(focalPt: Offset)
    fun isListeningForGestures(): Boolean
    fun shouldConsumeTapGesture(focalPt: Offset): Boolean
    fun shouldConsumeLongPress(focalPt: Offset): Boolean
}

internal interface LayoutSizeChangeListener {
    fun onSizeChanged(composableScope: CoroutineScope, size: IntSize)
}
