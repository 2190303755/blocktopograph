package ovh.plrapps.mapcompose.ui.markers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import ovh.plrapps.mapcompose.ui.layout.grid
import ovh.plrapps.mapcompose.ui.state.ZoomPanState
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData
import kotlin.math.ceil

@Composable
internal fun MarkerLayout(
    modifier: Modifier,
    zoomPanState: ZoomPanState,
    content: @Composable () -> Unit
) {
    /* Scroll values may not be represented accurately using floats (a float has 7 significant
     * decimal digits, so any number above ~10M isn't represented accurately).
     * Since the translate function of the Canvas works with floats, we perform a change of
     * referential so that we only need to translate the canvas by an amount which can be
     * precisely represented as a float. */
    val origin by remember {
        derivedStateOf {
            IntOffset(
                ((ceil(zoomPanState.cameraX / grid) * grid)).toInt(),
                ((ceil(zoomPanState.cameraY / grid) * grid)).toInt()
            )
        }
    }

    val density = LocalDensity.current
    Layout(
        content = content,
        modifier
            .graphicsLayer {
                // fixme
                translationX = -((zoomPanState.cameraX - origin.x) * zoomPanState.scale).toFloat()
                translationY = -((zoomPanState.cameraY - origin.y) * zoomPanState.scale).toFloat()
            }
            .background(Color.Transparent)
            .fillMaxSize()
    ) { measurables, constraints ->
        val placeableCst = constraints.copy(minHeight = 0, minWidth = 0)

        layout(constraints.maxWidth, constraints.maxHeight) {
            for (measurable in measurables) {
                val data = measurable.layoutId as? MarkerData ?: continue

                /* Don't layout markers which are way out of display bounds, as it can can cause
                 * jitter in marker rendering. */
                if (data.isOutOfDisplay()) continue

                val placeable = measurable.measure(placeableCst)
                data.measuredWidth = placeable.measuredWidth
                data.measuredHeight = placeable.measuredHeight

                val widthOffset =
                    placeable.measuredWidth * data.relativeOffset.x + with(density) { data.absoluteOffset.x.toPx() }
                val heightOffset =
                    placeable.measuredHeight * data.relativeOffset.y + with(density) { data.absoluteOffset.y.toPx() }

                val x = data.x * zoomPanState.scale + widthOffset
                val y = data.y * zoomPanState.scale + heightOffset
                /* It's important to always update data even when visibility is set to false, so
                 * click handling works on updated data (a non-visible marker might be clickable) */
                data.xPlacement = x
                data.yPlacement = y

                if (data.isVisible) {
                    placeable.place(
                        (x - origin.x).toInt(),
                        (y - origin.y).toInt(),
                        zIndex = data.zIndex
                    )
                }
            }
        }
    }
}

private fun MarkerData.isOutOfDisplay() = x < -1.0 || x > 2.0 || y < -1.0 || y > 2.0