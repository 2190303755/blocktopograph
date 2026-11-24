package ovh.plrapps.mapcompose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ovh.plrapps.mapcompose.ui.layout.ZoomPan
import ovh.plrapps.mapcompose.ui.state.MapModel
import ovh.plrapps.mapcompose.ui.view.TileCanvas

@Composable
fun MapUI(
    modifier: Modifier = Modifier,
    model: MapModel,
    visibleAreaPadding: (Density, IntSize) -> IntOffset = { _, _ -> IntOffset.Zero },
    onTap: (x: Double, y: Double) -> Unit = { _, _ -> },
    onLongPress: (x: Double, y: Double) -> Unit = { _, _ -> },
    onTouchDown: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    key(model) {
        ZoomPan(
            modifier = modifier, // clip/background are controlled by the caller
            zoomPanState = model.zoomPanState,
            visibleAreaPadding = visibleAreaPadding,
            onTap = onTap,
            onLongPress = onLongPress,
            onTouchDown = onTouchDown
        ) {
            val canvas = model.tileCanvasState
            TileCanvas(
                modifier = Modifier,
                zoomPanState = model.zoomPanState,
                visibleTilesResolver = model.visibleTilesResolver,
                tileSize = model.tileSize,
                alphaTick = canvas.alphaTick,
                colorFilterProvider = canvas.colorFilterProvider,
                tilesToRender = canvas.tilesToRender,
            )

            content()
        }
    }
}
