package com.mithrilmania.blocktopograph.ui.screen.world.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Label
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxOfOrDefault
import com.mithrilmania.blocktopograph.map.selection.Selection
import ovh.plrapps.mapcompose.api.cameraX
import ovh.plrapps.mapcompose.api.cameraY
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapState


/**
 * Overlay that renders the current [selection] on top of the map and lets the user resize it by
 * dragging the four edge handles.
 *
 * Coordinate conversion mirrors [CoordinateGridOverlay]: a world point (x, z) is projected to the
 * screen as `(halfWidth + (x - cameraX) * scale, halfHeight + (z - cameraY) * scale)`, where
 * `cameraX`/`cameraY` is the viewport center in world coordinates (blocks) and `scale` is pixels
 * per block.
 *
 * The four edge handles are positioned via a custom [Layout] [androidx.compose.ui.layout.MeasurePolicy]
 * instead of `onSizeChanged` + `offset`, so their placement is computed directly from the measured
 * layout size. The dark mask rectangle is drawn by a [Canvas] which reads its own layout size from
 * `DrawScope.size` (same as [CoordinateGridOverlay]).
 */
@Composable
fun SelectionOverlay(mapState: MapState, selection: Selection) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = mapState.scale.toFloat()
        val centerX = this.size.width * 0.5F
        val centerY = this.size.height * 0.5F

        val left = centerX + (selection.left - mapState.cameraX).toFloat() * scale
        val top = centerY + (selection.top - mapState.cameraY).toFloat() * scale

        val offset = Offset(left, top)
        val bound = Size(
            ((selection.right - selection.left) * scale).coerceAtLeast(0f),
            ((selection.bottom - selection.top) * scale).coerceAtLeast(0f)
        )
        drawRect(
            color = Color(0x80000000),
            topLeft = offset,
            size = bound
        )
        drawRect(
            color = Color.White,
            topLeft = offset,
            size = bound,
            style = Stroke(width = 1F)
        )
    }
}

@Composable
fun SelectionHandles(mapState: MapState, selection: Selection) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            // Children order: LEFT, TOP, RIGHT, BOTTOM (after the Canvas).
            DragHandleButton(
                mapState = mapState,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                coordinate = selection.left,
                orientation = Orientation.Horizontal
            ) { delta ->
                val edge = selection.left + delta
                selection.left = if (edge < selection.right) {
                    edge
                } else {
                    selection.right - 1
                }
            }

            DragHandleButton(
                mapState = mapState,
                icon = Icons.Filled.KeyboardArrowUp,
                coordinate = selection.top,
                orientation = Orientation.Vertical
            ) { delta ->
                val edge = selection.top + delta
                selection.top = if (edge < selection.bottom) {
                    edge
                } else {
                    selection.bottom - 1
                }
            }

            DragHandleButton(
                mapState = mapState,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                coordinate = selection.right,
                orientation = Orientation.Horizontal
            ) { delta ->
                val edge = selection.right + delta
                selection.right = if (edge > selection.left) {
                    edge
                } else {
                    selection.left + 1
                }
            }

            DragHandleButton(
                mapState = mapState,
                icon = Icons.Filled.KeyboardArrowDown,
                coordinate = selection.bottom,
                orientation = Orientation.Vertical
            ) { delta ->
                val edge = selection.bottom + delta
                selection.bottom = if (edge > selection.top) {
                    edge
                } else {
                    selection.top + 1
                }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val scale = mapState.scale.toFloat()
        val cameraX = mapState.cameraX.toFloat()
        val cameraY = mapState.cameraY.toFloat()

        val centerScreenX = width * 0.5F
        val centerScreenY = height * 0.5F

        // Selection bounds in screen pixels.
        val leftPx = centerScreenX + (selection.left - cameraX) * scale
        val rightPx = centerScreenX + (selection.right - cameraX) * scale
        val topPx = centerScreenY + (selection.top - cameraY) * scale
        val bottomPx = centerScreenY + (selection.bottom - cameraY) * scale

        // Measure the handles first so their real screen size drives the spacing rules. The
        // selection's screen size already incorporates mapState.scale, so basing the thresholds
        // on the measured handle size (instead of fixed dp) keeps them correct at any zoom level
        // and prevents the handles from overlapping when the selection renders small.
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(looseConstraints) }

        val handleWidth = placeables.fastMaxOfOrDefault(0) { it.width }.toFloat()
        val handleHeight = placeables.fastMaxOfOrDefault(0) { it.height }.toFloat()
        val handleHalf = maxOf(handleWidth, handleHeight) * 0.5F

        val spacing = 8.dp.toPx()

        // A handle center must stay at least half a handle (plus a small margin) away from the
        // selection edge so the button never spills outside it.
        val buttonToBoundMinDist = handleHalf + spacing

        // Opposite handles must be at least a full handle (plus margin) apart center-to-center.
        val minDistDraggers = maxOf(handleWidth, handleHeight) + spacing

        // Horizontal position of the top/bottom handles and vertical position of the left/right
        // handles.
        var horix = (leftPx.coerceAtLeast(0f) + rightPx.coerceAtMost(width)) / 2f
        var verty = (topPx.coerceAtLeast(0f) + bottomPx.coerceAtMost(height)) / 2f

        if (rightPx - leftPx > buttonToBoundMinDist * 2f) {
            if (horix - leftPx < buttonToBoundMinDist) horix = leftPx + buttonToBoundMinDist
            if (rightPx - horix < buttonToBoundMinDist) horix = rightPx - buttonToBoundMinDist
        }
        if (bottomPx - topPx > buttonToBoundMinDist * 2f) {
            if (verty - topPx < buttonToBoundMinDist) verty = topPx + buttonToBoundMinDist
            if (bottomPx - verty < buttonToBoundMinDist) verty = bottomPx - buttonToBoundMinDist
        }

        // Prevent opposite handles from collapsing onto each other when the selection is small.
        // Spread them by exactly the gap needed to reach minDistDraggers, so the transition is
        // smooth instead of jumping by a fixed half-distance.
        var horit = topPx
        var horib = bottomPx
        var vertl = leftPx
        var vertr = rightPx
        if (horib - horit < minDistDraggers) {
            val spread = (minDistDraggers - (horib - horit)) * 0.5F
            horit -= spread
            horib += spread
        }
        if (vertr - vertl < minDistDraggers) {
            val spread = (minDistDraggers - (vertr - vertl)) * 0.5F
            vertl -= spread
            vertr += spread
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            place(placeables[0], vertl.toInt(), verty.toInt())
            place(placeables[1], horix.toInt(), horit.toInt())
            place(placeables[2], vertr.toInt(), verty.toInt())
            place(placeables[3], horix.toInt(), horib.toInt())
        }
    }
}

fun Placeable.PlacementScope.place(placeable: Placeable, centerX: Int, centerY: Int) {
    placeable.placeRelative(centerX - placeable.width / 2, centerY - placeable.height / 2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DragHandleButton(
    mapState: MapState,
    icon: ImageVector,
    orientation: Orientation,
    coordinate: Int,
    onDrag: (Int) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Label(
        label = {
            PlainTooltip(
                modifier = Modifier
                    .sizeIn(45.dp)
                    .wrapContentWidth()
            ) {
                Text(coordinate.toString())
            }
        },
        interactionSource = interactionSource
    ) {
        // Accumulated drag distance in pixels, used to move in whole blocks while preserving the
        // sub-block remainder for smooth dragging.
        var delta by remember { mutableFloatStateOf(0f) }
        FilledIconButton(
            onClick = {},
            modifier = Modifier.draggable(
                state = rememberDraggableState {
                    val pos = delta + it
                    val scale = mapState.scale.toFloat()
                    val blocks = (pos / scale).toInt()
                    if (blocks == 0) {
                        delta = pos
                    } else {
                        delta = pos - blocks * scale
                        onDrag(blocks)
                    }
                },
                interactionSource = interactionSource,
                orientation = orientation,
                onDragStarted = { delta = 0F },
                onDragStopped = { delta = 0F }
            ),
            interactionSource = interactionSource
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
}
