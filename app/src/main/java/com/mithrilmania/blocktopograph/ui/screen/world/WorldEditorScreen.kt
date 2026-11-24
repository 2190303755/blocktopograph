package com.mithrilmania.blocktopograph.ui.screen.world

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.marker.BuiltinMarkerSource
import com.mithrilmania.blocktopograph.marker.ChunkedMarkerLayer
import com.mithrilmania.blocktopograph.marker.MapMarker
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.component.DragHandleConsumedHeight
import com.mithrilmania.blocktopograph.ui.component.FloatingPanelScaffold
import com.mithrilmania.blocktopograph.ui.component.safeLayoutInsets
import com.mithrilmania.blocktopograph.ui.screen.world.layer.BACKGROUND_SHADER
import com.mithrilmania.blocktopograph.ui.screen.world.layer.ERROR_SHADER
import com.mithrilmania.blocktopograph.ui.screen.world.layer.renderSatellite
import com.mithrilmania.blocktopograph.ui.screen.world.overlay.CoordinateGridOverlay
import com.mithrilmania.blocktopograph.ui.screen.world.overlay.SelectionHandles
import com.mithrilmania.blocktopograph.ui.screen.world.overlay.SelectionOverlay
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.CHUNK_DIMENSION
import com.mithrilmania.blocktopograph.world.LoadedWorld
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.chunk.ChunkPos
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.clearLayer
import ovh.plrapps.mapcompose.api.layerId
import ovh.plrapps.mapcompose.api.reloadTiles
import ovh.plrapps.mapcompose.api.setCamera
import ovh.plrapps.mapcompose.api.setLayer
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapModel
import android.graphics.Paint as AndroidPaint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldEditorScreen(loadedWorld: LoadedWorld, onNavigate: (AppNavKey) -> Unit) {
    val context = LocalContext.current
    val cutout = rememberUpdatedState(safeLayoutInsets())
    val map: MapModel = viewModel {
        MapModel(
            levelCount = ZOOM_LEVELS,
            tileSize = CHUNK_DIMENSION,
            scale = 16.0,
            minScale = 0.625,
            maxScale = 64.0,
            magnifyingFactor = 4,
        )
    }
    val editor = viewModel {
        val spawnPos = loadedWorld.spawnPos
        val localPlayer = loadedWorld.localPlayer
        val focusX = (localPlayer?.x ?: spawnPos.x.toFloat()).toDouble()
        val focusZ = (localPlayer?.z ?: spawnPos.z.toFloat()).toDouble()
        val dimension = if (localPlayer === null) {
            loadedWorld.dimensions[spawnPos.dimensionId]
        } else {
            loadedWorld.dimensions[localPlayer.dimensionId]
        }
        if (dimension !== null) {
            map.setCamera(focusX, focusZ)
        }
        WorldEditorModel(
            loadedWorld,
            dimension ?: VanillaDimension.OVERWORLD
        ) { density, height, floating ->
            height - density.run {
                DragHandleConsumedHeight.toPx()
            } - if (floating) 0 else cutout.value.getBottom(density)
        }
    }
    LaunchedEffect(Unit) {
        for (intent in editor.intents) {
            context.startActivity(intent)
        }
    }
    LaunchedEffect(editor.dimension) {
        map.reloadTiles()
    }
    DisposableEffect(Unit) {
        // TODO: it is too loooooooooooooooooooooooooooooong
        if (map.layerId === null) {
            map.setLayer("major") { row, col, zoomLvl ->
                val chunks = 1 shl (ZOOM_LEVELS - zoomLvl - 1)
                val tileSize = CHUNK_DIMENSION * chunks
                val cache = editor.chunks
                val dimension = editor.dimension
                val bitmap = createBitmap(
                    tileSize,
                    tileSize,
                    Bitmap.Config.RGB_565
                )
                val canvas = Canvas(bitmap)
                val paint = AndroidPaint()
                paint.setShader(BACKGROUND_SHADER)
                canvas.drawRect(0F, 0F, tileSize.toFloat(), tileSize.toFloat(), paint)
                val context = currentCoroutineContext()
                for (offsetX in 0 until chunks) {
                    val left = offsetX * CHUNK_DIMENSION
                    val chunkX = offsetX + col * chunks
                    for (offsetZ in 0 until chunks) {
                        val top = offsetZ * CHUNK_DIMENSION
                        context.ensureActive()
                        val pos = ChunkPos(
                            dimension.runtimeId,
                            chunkX,
                            offsetZ + row * chunks
                        )
                        val chunk = cache.getAsync(pos).await() ?: continue
                        try {
                            renderSatellite(
                                bitmap,
                                editor.chunks,
                                chunk,
                                left,
                                top
                            )
                        } catch (e: Exception) {
                            Log.e(APP_TAG, "Failed to render chunk at $pos", e)
                            paint.setShader(ERROR_SHADER)
                            canvas.drawRect(
                                left.toFloat(),
                                top.toFloat(),
                                (left + CHUNK_DIMENSION).toFloat(),
                                (top + CHUNK_DIMENSION).toFloat(),
                                paint
                            )
                        }
                    }
                }
                bitmap
            }
            // TODO: auto create markers
        }
        onDispose {
            map.clearLayer()
        }
    }
    val isPanelAction = remember {
        derivedStateOf {
            editor.selection.isSelecting
        }
    }
    FloatingPanelScaffold(
        scaffoldState = editor.scaffoldState,
        panelContent = { padding ->
            Column {
                BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
                AnimatedContent(
                    targetState = editor.selection.isSelecting,
                    transitionSpec = {
                        (fadeIn(tween(220)) +
                                slideInVertically(tween(220)) { it / 4 }) togetherWith
                                (fadeOut(tween(150)) +
                                        slideOutVertically(tween(150)) { it / 4 })
                    },
                    label = "worldEditorPanelContent"
                ) { isSelecting ->
                    if (isSelecting) {
                        SelectionPanel(editor)
                    } else {
                        Column {
                            PrimaryTabRow(selectedTabIndex = editor.tabPager.currentPage) {
                                val coroutineScope = rememberCoroutineScope()
                                arrayOf(
                                    "视图",
                                    "标记",
                                    "数据"
                                ).forEachIndexed { index, title ->
                                    Tab(
                                        selected = editor.tabPager.currentPage == index,
                                        onClick = {
                                            coroutineScope.launch {
                                                editor.tabPager.animateScrollToPage(index)
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = title.todoI18n(),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                            }
                            HorizontalPager(editor.tabPager) {
                                when (it) {
                                    1 -> MarkerTab(map, editor, padding)
                                    2 -> StorageTab(map, editor, padding, onNavigate)
                                    else -> ViewModeTab(map, editor, padding)
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        isPanelAction = isPanelAction,
        action = {
            val density = LocalDensity.current
            AnimatedContent(
                targetState = isPanelAction.value,
                transitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start
                    ) {
                        it + density.run { 16.dp.roundToPx() }
                    } togetherWith slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End
                    ) {
                        it + density.run { 16.dp.roundToPx() }
                    } using null
                },
                contentAlignment = Alignment.BottomEnd
            ) {
                if (it) {
                    SelectionAction(map, editor)
                } else {
                    FloatingActionButton(
                        onClick = { context.upcoming() },
                        Modifier.animateFloatingActionButton(
                            !isPanelAction.value,
                            Alignment.BottomEnd
                        )
                    ) {
                        Icon(Icons.Filled.Save, null)
                    }
                }
            }
        }
    ) {
        MapUI(
            model = map,
            onLongPress = { x, y -> editor.onMapLongPress(x, y) },
            modifier = Modifier
                .background(Color(0xFFD6BE96))
                .clipToBounds()
        ) {
            if (editor.showGrid) {
                CoordinateGridOverlay(map)
            }
            if (editor.selection.isSelecting) {
                SelectionOverlay(map, editor.selection)
                SelectionHandles(map, editor.selection)
            }
            BuiltinMarkers(map, editor)
        }
    }
    LongPressMenu(map, editor, onNavigate)
    SelectionJobIndicator(editor)
}

@Composable
private fun BuiltinMarkers(mapModel: MapModel, editor: WorldEditorModel) {
    val source = remember {
        BuiltinMarkerSource(buildList {
            val world = editor.world
            val dimensions = world.dimensions
            val spawnPos = world.spawnPos
            dimensions[spawnPos.dimensionId]?.let { dimension ->
                add(MapMarker.Spawn(spawnPos.x, spawnPos.z, dimension))
            }
            world.localPlayer?.let { player ->
                dimensions[player.dimensionId]?.let { dimension ->
                    add(MapMarker.Player(null, player.x.toInt(), player.z.toInt(), dimension))
                }
            }
        })
    }
    ChunkedMarkerLayer(
        model = mapModel,
        dimensionId = editor.dimension.runtimeId,
        source = source,
    )
}
