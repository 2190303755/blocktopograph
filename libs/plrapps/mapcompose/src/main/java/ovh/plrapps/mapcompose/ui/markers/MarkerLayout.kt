package ovh.plrapps.mapcompose.ui.markers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import ovh.plrapps.mapcompose.ui.state.ZoomPanState
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData
import kotlin.math.absoluteValue

@Composable
internal fun MarkerLayout(
    modifier: Modifier,
    zoomPanState: ZoomPanState,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    Layout(
        content = content,
        modifier
            .background(Color.Transparent)
            .fillMaxSize()
    ) { measurables, constraints ->
        val placeableCst = constraints.copy(minHeight = 0, minWidth = 0)
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            val cameraX = zoomPanState.cameraX
            val cameraY = zoomPanState.cameraY
            val scale = zoomPanState.scale
            val halfWidth = width / 2
            val halfHeight = height / 2
            for (measurable in measurables) {
                val data = measurable.layoutId as? MarkerData ?: continue
                val offsetX = (data.x - cameraX) * scale
                val offsetY = (data.y - cameraY) * scale
                if (offsetX.absoluteValue > halfWidth || offsetY.absoluteValue > halfHeight) continue

                val placeable = measurable.measure(placeableCst)
                data.measuredWidth = placeable.measuredWidth
                data.measuredHeight = placeable.measuredHeight

                val widthOffset =
                    placeable.measuredWidth * data.relativeOffset.x + with(density) { data.absoluteOffset.x.toPx() }
                val heightOffset =
                    placeable.measuredHeight * data.relativeOffset.y + with(density) { data.absoluteOffset.y.toPx() }

                val x = offsetX + widthOffset + halfWidth
                val y = offsetY + heightOffset + halfHeight
                /* It's important to always update data even when visibility is set to false, so
                 * click handling works on updated data (a non-visible marker might be clickable) */
                data.xPlacement = x
                data.yPlacement = y

                if (data.isVisible) {
                    placeable.place(
                        x.toInt(),
                        y.toInt(),
                        zIndex = data.zIndex
                    )
                }
            }
        }
    }
}
