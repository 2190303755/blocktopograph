package com.mithrilmania.blocktopograph.ui.screen.world.overlay

import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.mithrilmania.blocktopograph.world.CHUNK_DIMENSION
import ovh.plrapps.mapcompose.api.cameraX
import ovh.plrapps.mapcompose.api.cameraY
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.floor
import kotlin.math.log2
import android.graphics.Paint as AndroidPaint

@Composable
fun CoordinateGridOverlay(mapState: MapState) {
    val textPaint = remember {
        TextPaint(
            AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.LINEAR_TEXT_FLAG
        ).apply {
            style = AndroidPaint.Style.FILL
            color = -1
        }
    }
    val step by remember {
        derivedStateOf {
            val scale = mapState.scale
            val shift = floor(4.75 - log2(scale)).toInt()
            if (shift > 0) 1 shl shift else 1
        }
    }
    val textStyle = MaterialTheme.typography.labelMedium
    Canvas(modifier = Modifier.fillMaxSize()) {
        val textSize = textStyle.fontSize.toPx()
        val lineHeight = textStyle.lineHeight.toPx()
        val indent = lineHeight - textSize
        textPaint.textSize = textSize
        val scale = mapState.scale.toFloat()
        val cameraX = mapState.cameraX.toFloat()
        val cameraY = mapState.cameraY.toFloat()
        val layoutSize = size
        val gridStepInChunks = step

        val centerScreenX = layoutSize.width * 0.5F
        val centerScreenY = layoutSize.height * 0.5F

        val startChunkX =
            ((cameraX - centerScreenX / scale) / CHUNK_DIMENSION).toInt() - 1
        val endChunkX =
            ((cameraX + centerScreenX / scale) / CHUNK_DIMENSION).toInt() + 1
        val startChunkZ =
            ((cameraY - centerScreenY / scale) / CHUNK_DIMENSION).toInt() - 1
        val endChunkZ =
            ((cameraY + centerScreenY / scale) / CHUNK_DIMENSION).toInt() + 1

        val gridStartChunkX =
            (startChunkX / gridStepInChunks - 1) * gridStepInChunks
        val gridStartChunkZ =
            (startChunkZ / gridStepInChunks - 1) * gridStepInChunks

        for (chunkX in gridStartChunkX..endChunkX step gridStepInChunks) {
            val screenX =
                centerScreenX + (chunkX * CHUNK_DIMENSION - cameraX) * scale
            drawLine(
                start = Offset(screenX, 0f),
                end = Offset(screenX, layoutSize.height),
                color = Color.White
            )
        }

        for (chunkZ in gridStartChunkZ..endChunkZ step gridStepInChunks) {
            val screenY =
                centerScreenY + (chunkZ * CHUNK_DIMENSION - cameraY) * scale
            drawLine(
                start = Offset(0f, screenY),
                end = Offset(layoutSize.width, screenY),
                color = Color.White
            )
        }

        drawIntoCanvas { wrapper ->
            val canvas = wrapper.nativeCanvas
            for (chunkZ in gridStartChunkZ..endChunkZ step gridStepInChunks) {
                val screenY =
                    centerScreenY + (chunkZ * CHUNK_DIMENSION - cameraY) * scale + lineHeight
                for (chunkX in gridStartChunkX..endChunkX step gridStepInChunks) {
                    val screenX =
                        centerScreenX + (chunkX * CHUNK_DIMENSION - cameraX) * scale
                    canvas.drawText(
                        "(${chunkX * CHUNK_DIMENSION}; ${chunkZ * CHUNK_DIMENSION})",
                        screenX + indent,
                        screenY,
                        textPaint
                    )
                }
            }
        }
    }
}