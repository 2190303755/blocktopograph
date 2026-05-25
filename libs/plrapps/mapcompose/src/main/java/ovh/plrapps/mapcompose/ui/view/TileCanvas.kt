package ovh.plrapps.mapcompose.ui.view

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import ovh.plrapps.mapcompose.core.ColorFilterProvider
import ovh.plrapps.mapcompose.core.Tile
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.ui.layout.grid
import ovh.plrapps.mapcompose.ui.state.ZoomPanState
import kotlin.math.ceil

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    zoomPanState: ZoomPanState,
    visibleTilesResolver: VisibleTilesResolver,
    tileSize: Int,
    alphaTick: Float,
    colorFilterProvider: ColorFilterProvider?,
    tilesToRender: List<Tile>,
    isFilteringBitmap: () -> Boolean,
) {
    val dest = remember { Rect() }
    val paint: Paint = remember {
        Paint().apply {
            isAntiAlias = false
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        /* Scroll values may not be represented accurately using floats (a float has 7 significant
         * decimal digits, so any number above ~10M isn't represented accurately).
         * Since the translate function of the Canvas works with floats, we perform a change of
         * referential so that we only need to translate the canvas by an amount which can be
         * precisely represented as a float. */
        val refX = ceil(zoomPanState.cameraX / grid).toInt() * grid
        val refY = ceil(zoomPanState.cameraY / grid).toInt() * grid

        withTransform({
            /* Geometric transformations seem to be applied in reversed order of declaration */
            translate(
                left = size.width / 2.0F - ((zoomPanState.cameraX - refX) * zoomPanState.scale).toFloat(),
                top = size.height / 2.0F - ((zoomPanState.cameraY - refY) * zoomPanState.scale).toFloat()
            )
            scale(scale = zoomPanState.scale.toFloat(), Offset.Zero)
        }) {
            paint.isFilterBitmap = isFilteringBitmap()

            for (tile in tilesToRender) {
                if (tile.markedForSweep) continue
                val bitmap = tile.bitmap ?: continue
                val scaleForLevel = visibleTilesResolver.getScaleForLevel(tile.zoom)
                val tileScaled = (tileSize / scaleForLevel).toInt()

                drawTile(
                    tile = tile,
                    tileScaled = tileScaled,
                    refX = refX,
                    refY = refY,
                    dest = dest,
                    colorFilterProvider = colorFilterProvider,
                    paint = paint,
                    bitmap = bitmap,
                )

                /* If a tile isn't fully opaque, increase its alpha state by the alpha tick */
                if (tile.alpha < 1f) {
                    tile.alpha = (tile.alpha + alphaTick).coerceAtMost(1f)
                } else {
                    tile.overlaps?.markedForSweep = true
                    tile.overlaps = null
                }
            }
        }
    }
}

private fun DrawScope.drawTile(
    tile: Tile,
    tileScaled: Int,
    refX: Int,
    refY: Int,
    dest: Rect,
    colorFilterProvider: ColorFilterProvider?,
    paint: Paint,
    bitmap: Bitmap,
) {
    /* The change of referential is done by offsetting coordinates by (refX, refY) */
    val left = tile.col * tileScaled - refX
    val top = tile.row * tileScaled - refY
    dest.set(left, top, left + tileScaled, top + tileScaled)

    val colorFilter = colorFilterProvider?.getColorFilter(tile.row, tile.col, tile.zoom)

    paint.alpha = (tile.alpha * 255).toInt()
    paint.colorFilter = colorFilter?.asAndroidColorFilter()

    drawIntoCanvas {
        it.nativeCanvas.drawBitmap(bitmap, null, dest, paint)
    }
}
