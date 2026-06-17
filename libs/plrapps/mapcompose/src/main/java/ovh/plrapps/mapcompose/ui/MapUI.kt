package ovh.plrapps.mapcompose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import ovh.plrapps.mapcompose.ui.layout.ZoomPan
import ovh.plrapps.mapcompose.ui.markers.MarkerComposer
import ovh.plrapps.mapcompose.ui.paths.PathComposer
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.view.TileCanvas

@Composable
fun MapUI(
    modifier: Modifier = Modifier,
    state: MapState,
    content: @Composable () -> Unit = {}
) {
    val zoomPanState = state.zoomPanState
    val markerState = state.markerRenderState
    val pathState = state.pathState

    key(state) {
        ZoomPan(
            modifier = modifier, // if someone wants to clip or change the background, just pass modifier
            gestureListener = zoomPanState,
            layoutSizeChangeListener = zoomPanState,
        ) {
            TileCanvas(
                modifier = Modifier,
                zoomPanState = zoomPanState,
                visibleTilesResolver = state.visibleTilesResolver,
                tileSize = state.tileSize,
                alphaTick = state.tileCanvasState.alphaTick,
                colorFilterProvider = state.tileCanvasState.colorFilterProvider,
                tilesToRender = state.tileCanvasState.tilesToRender,
                isFilteringBitmap = state.isFilteringBitmap,
            )

            MarkerComposer(
                modifier = Modifier.zIndex(1f),
                zoomPanState = zoomPanState,
                markerRenderState = markerState,
                mapState = state
            )

            PathComposer(
                modifier = Modifier,
                zoomPanState = zoomPanState,
                pathState = pathState
            )

            content()
        }
    }
}