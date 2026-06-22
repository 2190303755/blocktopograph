package com.mithrilmania.blocktopograph.editor.world.v2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composeunstyled.SheetDetent
import com.composeunstyled.rememberBottomSheetState
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditor
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.editor.world.v2.layer.BACKGROUND_SHADER
import com.mithrilmania.blocktopograph.editor.world.v2.layer.ERROR_SHADER
import com.mithrilmania.blocktopograph.editor.world.v2.layer.renderSatellite
import com.mithrilmania.blocktopograph.map.CustomIcon
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readNamedTag
import com.mithrilmania.blocktopograph.ui.component.BottomSheet
import com.mithrilmania.blocktopograph.ui.component.DragHandleConsumedHeight
import com.mithrilmania.blocktopograph.ui.component.Marker
import com.mithrilmania.blocktopograph.ui.theme.setThemedContent
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.math.DimensionVec3i
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.GlobalKey
import com.mithrilmania.blocktopograph.world.HeightRange
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.buildDimensionRegistry
import com.mithrilmania.blocktopograph.world.chunk.ChunkPos
import com.mithrilmania.blocktopograph.world.extractPlayerPos
import com.mithrilmania.blocktopograph.world.get
import com.mithrilmania.blocktopograph.world.resolveWorld
import it.unimi.dsi.fastutil.longs.Long2IntMaps
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.cameraX
import ovh.plrapps.mapcompose.api.cameraY
import ovh.plrapps.mapcompose.api.clearLayer
import ovh.plrapps.mapcompose.api.layerId
import ovh.plrapps.mapcompose.api.reloadTiles
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.setLayer
import ovh.plrapps.mapcompose.ui.MapUI
import java.io.ByteArrayInputStream
import kotlin.math.floor
import kotlin.math.log2
import android.graphics.Paint as AndroidPaint

class WorldEditorActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[WorldEditorModel::class.java]
        if (viewModel.initialization === InitState.Uninitialized) {
            val world = this.intent.resolveWorld(this)
            if (world === null) {
                Log.e(APP_TAG, "Failed to open world")
                Toast.makeText(this, "Invalid world", Toast.LENGTH_SHORT).show()
                //WTF, try going back to the previous screen by finishing this hopeless activity...
                this.finish()
                return
            }
            viewModel.viewModelScope.launch {
                viewModel.initialization = InitState.Initializing
                val storage = withContext(Dispatchers.IO) {
                    world.open(viewModel.application)
                }
                if (storage === null) {
                    viewModel.initialization = InitState.Failed
                    return@launch
                }
                val dimensions = async(Dispatchers.IO) {
                    buildDimensionRegistry(storage.db[GlobalKey.DIMENSION_REGISTRY])
                }
                val heightBounds = async(Dispatchers.IO) {
                    val bytes = storage.db[GlobalKey.CHUNK_METAS]
                    if (bytes === null) Long2IntMaps.EMPTY_MAP else {
                        val input = BedrockNBTInput(ByteArrayInputStream(bytes))
                        val size = input.readInt()
                        val ranges = Long2IntOpenHashMap(size)
                        repeat(size) {
                            val hash = input.readLong()
                            val range = (input.readNamedTag().second as? CompoundTag)
                                ?.get("LastSavedDimensionHeightRange")
                            if (range is CompoundTag) {
                                ranges.put(
                                    hash,
                                    HeightRange(
                                        range.getTyped<NumericTag>("min")?.toInt() ?: 0,
                                        range.getTyped<NumericTag>("max")?.toInt() ?: 0
                                    ).packed
                                )
                            }
                        }
                        ranges
                    }
                }
                val spawnPos = async(Dispatchers.IO) {
                    val root = world.config.getCached(viewModel.application)
                    DimensionVec3i(
                        VanillaDimension.OVERWORLD.runtimeId,
                        root.getTyped<NumericTag>("SpawnX")?.toInt() ?: 0,
                        root.getTyped<NumericTag>("SpawnY")?.toInt()
                            ?: 0, // 32767 is not the fallback
                        root.getTyped<NumericTag>("SpawnZ")?.toInt() ?: 0
                    )
                }
                val localPlayer = async(Dispatchers.IO) {
                    try {
                        val bytes: ByteArray? = storage.db[GlobalKey.LOCAL_PLAYER]
                        val player: BinaryTag? = if (bytes === null) {
                            world.config.getCached(viewModel.application)["Player"]
                        } else {
                            BedrockNBTInput(ByteArrayInputStream(bytes)).readNamedTag().second
                        }
                        if (player is CompoundTag) {
                            player.extractPlayerPos()
                        } else {
                            LogUtil.d(this, "No local player. A server world?")
                            null
                        }
                    } catch (e: Exception) {
                        LogUtil.d(this, e)
                        null
                    }
                }
                viewModel.loadWorld(
                    world,
                    storage,
                    spawnPos.await(),
                    dimensions.await(),
                    heightBounds.await(),
                    localPlayer.await()
                )
            }
        }
        this.setThemedContent {
            WorldEditorScaffold(viewModel) { info ->
                LaunchedEffect(viewModel.dimension) {
                    viewModel.map.reloadTiles()
                }
                DisposableEffect(Unit) {
                    // TODO: it is too loooooooooooooooooooooooooooooong
                    if (viewModel.map.layerId === null) {
                        viewModel.map.setLayer("major") { row, col, zoomLvl ->
                            val chunks = 1 shl (ZOOM_LEVELS - zoomLvl - 1)
                            val tileSize = CHUNK_DIMENSION * chunks
                            val cache = info.chunks
                            val dimension = viewModel.dimension
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
                                            info.chunks,
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

                        info.localPlayer?.let {
                            viewModel.map.addMarker(
                                "builtin:local_player",
                                it.x.toDouble(),
                                it.z.toDouble()
                            ) {
                                Marker(
                                    viewModel.entityIcons.value,
                                    IntOffset(112, 0),
                                    IntSize(16, 16),
                                    Modifier.size(16.dp)
                                )
                            }
                        }

                        val spawnPos = info.spawnPos
                        viewModel.map.addMarker(
                            "builtin:spawn_point",
                            spawnPos.x.toDouble(),
                            spawnPos.z.toDouble()
                        ) {
                            val spec = CustomIcon.SPAWN_MARKER.sprite
                            Marker(
                                viewModel.customIcons.value,
                                IntOffset(spec.left, spec.top),
                                IntSize(spec.width, spec.height),
                                Modifier.size(16.dp)
                            )
                        }
                    }
                    onDispose {
                        viewModel.map.clearLayer()
                    }
                }
                val cutout = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                val density = LocalDensity.current
                val collapse = SheetDetent("collapse") { _, _ ->
                    with(density) {
                        cutout.getBottom(this).toDp()
                    } + DragHandleConsumedHeight
                }
                val sheetState = rememberBottomSheetState(
                    collapse,
                    listOf(
                        collapse,
                        SheetDetent(
                            "partial-expanded-0.4"
                        ) { containerHeight, _ -> containerHeight * 0.4F },
                        SheetDetent(
                            "partial-expanded-0.6"
                        ) { containerHeight, _ -> containerHeight * 0.6F },
                        SheetDetent.FullyExpanded
                    )
                )
                NBTEditingHost(viewModel) {
                    Box(contentAlignment = Alignment.Center) {
                        MapUI(
                            state = viewModel.map,
                            modifier = Modifier.background(Color(0xFFD6BE96))
                        ) {
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
                                    val state = viewModel.map
                                    val scale = state.scale
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
                                val mapState = viewModel.map
                                val scale = mapState.scale.toFloat()
                                val cameraX = mapState.cameraX.toFloat()
                                val cameraY = mapState.cameraY.toFloat()
                                val layoutSize = size
                                val gridStepInChunks = step

                                val halfWidth = layoutSize.width * 0.5F
                                val halfHeight = layoutSize.height * 0.5F

                                val startChunkX =
                                    ((cameraX - halfWidth / scale) / CHUNK_DIMENSION).toInt() - 1
                                val endChunkX =
                                    ((cameraX + halfWidth / scale) / CHUNK_DIMENSION).toInt() + 1
                                val startChunkZ =
                                    ((cameraY - halfHeight / scale) / CHUNK_DIMENSION).toInt() - 1
                                val endChunkZ =
                                    ((cameraY + halfHeight / scale) / CHUNK_DIMENSION).toInt() + 1

                                val gridStartChunkX =
                                    (startChunkX / gridStepInChunks - 1) * gridStepInChunks
                                val gridStartChunkZ =
                                    (startChunkZ / gridStepInChunks - 1) * gridStepInChunks

                                for (chunkX in gridStartChunkX..endChunkX step gridStepInChunks) {
                                    val screenX =
                                        halfWidth + (chunkX * CHUNK_DIMENSION - cameraX) * scale
                                    drawLine(
                                        start = Offset(screenX, 0f),
                                        end = Offset(screenX, layoutSize.height),
                                        color = Color.White
                                    )
                                }

                                for (chunkZ in gridStartChunkZ..endChunkZ step gridStepInChunks) {
                                    val screenY =
                                        halfHeight + (chunkZ * CHUNK_DIMENSION - cameraY) * scale
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
                                            halfHeight + (chunkZ * CHUNK_DIMENSION - cameraY) * scale + lineHeight
                                        for (chunkX in gridStartChunkX..endChunkX step gridStepInChunks) {
                                            val screenX =
                                                halfWidth + (chunkX * CHUNK_DIMENSION - cameraX) * scale
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
                        BottomSheet(
                            sheetState = sheetState,
                            sheetContainerColor = MaterialTheme.colorScheme.background,
                            floatingContent = {
                                FloatingActionButton(
                                    onClick = { upcoming() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-16).dp, y = (-72).dp)
                                ) {
                                    Icon(Icons.Filled.Save, "")
                                }
                            }
                        ) {
                            PrimaryTabRow(selectedTabIndex = viewModel.tabPager.currentPage) {
                                val coroutineScope = rememberCoroutineScope()
                                arrayOf(
                                    "视图",
                                    "标记",
                                    "数据"
                                ).forEachIndexed { index, title ->
                                    Tab(
                                        selected = viewModel.tabPager.currentPage == index,
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.tabPager.animateScrollToPage(index)
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = title,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                    )
                                }
                            }
                            HorizontalPager(viewModel.tabPager) {
                                when (it) {
                                    1 -> MarkerTab(viewModel, info)
                                    2 -> StorageTab(viewModel, info)
                                    else -> ViewModeTab(viewModel, info)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorldEditorScaffold(
    viewModel: WorldEditorModel,
    content: @Composable (InitState.Succeed) -> Unit
) {
    AnimatedContent(
        targetState = viewModel.initialization,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
        transitionSpec = {
            fadeIn(animationSpec = tween(220, delayMillis = 90))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        }
    ) { init ->
        when (init) {
            is InitState.Succeed -> content(init)

            InitState.Failed -> {
                val context = LocalContext.current
                val owner = LocalOnBackPressedDispatcherOwner.current
                LaunchedEffect(Unit) {
                    Log.e(APP_TAG, "Failed to open storage")

                    Toast.makeText(
                        context,
                        "Missing leveldb",
                        Toast.LENGTH_SHORT
                    ).show()
                    owner?.onBackPressedDispatcher?.onBackPressed()
                }
            }

            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
    }
}

@Composable
fun NBTEditingHost(
    viewModel: WorldEditorModel,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = viewModel.editing.collectAsState().value,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(animationSpec = tween(220, delayMillis = 90))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        }
    ) {
        if (it === null) {
            content()
        } else {
            val editor = viewModel<NBTEditorModel>()
            val onBack: () -> Unit = {
                viewModel.viewModelScope.launch {
                    viewModel.editing.emit(null)
                }
                editor.navigation = null
            }
            BackHandler(true, onBack)
            NBTEditor(editor, onBack)
            LaunchedEffect(it) {
                if (editor.navigation == it) return@LaunchedEffect
                editor.viewModelScope.launch {
                    editor.readFromFile(it.first, it.second)
                }
                editor.navigation = it
            }
        }
    }
}

