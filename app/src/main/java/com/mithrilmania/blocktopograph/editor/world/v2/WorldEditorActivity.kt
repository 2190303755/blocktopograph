package com.mithrilmania.blocktopograph.editor.world.v2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.block.KnownBlockRepr
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditor
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.map.CustomIcon
import com.mithrilmania.blocktopograph.map.MCTileProvider
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.LocalPlayerSource
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.nbt.io.readNamedTag
import com.mithrilmania.blocktopograph.storage.file
import com.mithrilmania.blocktopograph.ui.component.Marker
import com.mithrilmania.blocktopograph.ui.component.PartiallyOrFullyExpanded
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.theme.setThemedContent
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.math.DimensionVector3
import com.mithrilmania.blocktopograph.world.extractPlayerPos
import com.mithrilmania.blocktopograph.world.resolveSpawnPoint
import com.mithrilmania.blocktopograph.world.resolveWorld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.MapUI
import java.io.ByteArrayInputStream
import java.io.IOException

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
                viewModel.initialization = if (storage === null) {
                    InitState.Failed
                } else {
                    InitState.Succeed(world, storage)
                }
            }
        }
        val assets = this.assets
        this.lifecycleScope.launch(Dispatchers.IO) {
            try {
                KnownBlockRepr.loadBitmaps(assets)
            } catch (e: IOException) {
                LogUtil.d(this, e)
            }
        }
        this.setThemedContent {
            WorldEditorScaffold(viewModel) { info ->
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    // TODO: it is too loooooooooooooooooooooooooooooong
                    if (viewModel.majorLayerId == null) {
                        viewModel.majorLayerId = viewModel.map.addLayer({ row, col, zoomLvl ->
                            val chunks = 1 shl (ZOOM_LEVELS - zoomLvl - 1)
                            val tileSize = TILE_DIMENSION * chunks
                            val storage = info.storage
                            val dimension = viewModel.dimension
                            val bitmap = createBitmap(
                                tileSize,
                                tileSize,
                                Bitmap.Config.RGB_565
                            )
                            val canvas = Canvas(bitmap)
                            val paint = Paint()
                            for (offsetX in 0 until chunks) {
                                val left = offsetX * TILE_DIMENSION
                                val chunkX = offsetX + col * chunks
                                for (offsetZ in 0 until chunks) {
                                    val top = offsetZ * TILE_DIMENSION
                                    val chunkZ = offsetZ + row * chunks
                                    val chunk = storage.getChunk(chunkX, chunkZ, dimension)
                                    if (chunk.isError) {
                                        MapType.ERROR.renderer.renderToBitmap(
                                            chunk,
                                            canvas,
                                            dimension,
                                            chunkX,
                                            chunkZ,
                                            left,
                                            top,
                                            RENDER_SCALE,
                                            RENDER_SCALE,
                                            paint,
                                            storage
                                        )
                                        continue
                                    }
                                    MapType.CHESS.renderer.renderToBitmap(
                                        chunk,
                                        canvas,
                                        dimension,
                                        chunkX,
                                        chunkZ,
                                        left,
                                        top,
                                        RENDER_SCALE,
                                        RENDER_SCALE,
                                        paint,
                                        storage
                                    )
                                    if (chunk.isVoid) continue
                                    try {
                                        MapType.OVERWORLD_SATELLITE.renderer.renderToBitmap(
                                            chunk,
                                            canvas,
                                            dimension,
                                            chunkX,
                                            chunkZ,
                                            left,
                                            top,
                                            RENDER_SCALE,
                                            RENDER_SCALE,
                                            paint,
                                            storage
                                        )
                                    } catch (e: Exception) {
                                        Log.e(
                                            APP_TAG,
                                            "Failed to render chunk at ($chunkX, $chunkZ)",
                                            e
                                        )
                                        MapType.ERROR.renderer.renderToBitmap(
                                            chunk,
                                            canvas,
                                            dimension,
                                            chunkX,
                                            chunkZ,
                                            left,
                                            top,
                                            RENDER_SCALE,
                                            RENDER_SCALE,
                                            paint,
                                            storage
                                        )
                                    }
                                }
                            }

                            //draw tile-edges white
                            val edge = tileSize.toFloat() - 1F
                            paint.setColor(-1)
                            canvas.drawLine(0F, 0F, edge, 1F, paint)
                            canvas.drawLine(0F, 0F, 1F, edge, paint)
                            canvas.drawLine(0F, edge, edge, edge, paint)
                            canvas.drawLine(edge, 0F, edge, edge, paint)

                            MCTileProvider.drawText(
                                "(${col * CHUNK_DIMENSION * chunks}; ${row * CHUNK_DIMENSION * chunks})",
                                bitmap,
                                -1,
                                0
                            )

                            bitmap
                        })
                        var framedToPlayer = false
                        try {
                            val playerPos: DimensionVector3<Float>? = try {
                                val data: ByteArray? =
                                    info.storage.db.get(SpecialDBEntryType.LOCAL_PLAYER.keyBytes)
                                val player: BinaryTag? = if (data === null) {
                                    info.world.config.getCached(this@WorldEditorActivity)["Player"]
                                } else {
                                    BedrockNBTInput(ByteArrayInputStream(data)).readNamedTag().second
                                }
                                if (player !is CompoundTag) {
                                    LogUtil.d(this, "No local player. A server world?")
                                    null
                                } else {
                                    player.extractPlayerPos()
                                }
                            } catch (e: Exception) {
                                LogUtil.d(this, e)
                                null
                            }
                            if (playerPos != null) {
                                val x: Float = playerPos.x
                                val y: Float = playerPos.y
                                val z: Float = playerPos.z
                                LogUtil.d(
                                    this,
                                    "Placed player marker at: $x;$y;$z [${playerPos.dimension.name}]"
                                )
                                viewModel.map.addMarker(
                                    "builtin:local_player",
                                    x.toDouble() * RENDER_SCALE,
                                    z.toDouble() * RENDER_SCALE
                                ) {
                                    Marker(
                                        viewModel.entityIcons.collectAsState().value,
                                        IntOffset(112, 0),
                                        IntSize(16, 16),
                                        Modifier.size(16.dp)
                                    )
                                }
                                if (playerPos.dimension != viewModel.dimension) {
                                    viewModel.dimension = playerPos.dimension
                                    //model.mapType.setValue(localPlayerMarker.dimension.defaultMapType)
                                }
                                coroutineScope.launch {
                                    viewModel.map.scrollTo(
                                        x.toDouble() * RENDER_SCALE,
                                        z.toDouble() * RENDER_SCALE
                                    )
                                }
                                framedToPlayer = true
                            }
                        } catch (e: Exception) {
                            LogUtil.d(this, "Failed to place player marker.", e)
                        }

                        try {
                            val spawnPos = info.world.resolveSpawnPoint(this@WorldEditorActivity)
                            viewModel.map.addMarker(
                                "builtin:spawn_point",
                                spawnPos.x.toDouble() * RENDER_SCALE,
                                spawnPos.z.toDouble() * RENDER_SCALE
                            ) {
                                val spec = CustomIcon.SPAWN_MARKER.sprite
                                Marker(
                                    viewModel.customIcons.collectAsState().value,
                                    IntOffset(spec.left, spec.top),
                                    IntSize(spec.width, spec.height),
                                    Modifier.size(16.dp)
                                )
                            }
                            if (!framedToPlayer) {
                                if (spawnPos.dimension != viewModel.dimension) {
                                    viewModel.dimension = spawnPos.dimension
                                    //model.mapType.setValue(localPlayerMarker.dimension.defaultMapType)
                                }
                                coroutineScope.launch {
                                    viewModel.map.scrollTo(
                                        spawnPos.x.toDouble() * RENDER_SCALE,
                                        spawnPos.z.toDouble() * RENDER_SCALE
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            LogUtil.d(this, "Failed to place spawn pos marker.", e)
                        }
                    }
                }
                val scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(
                        initialValue = SheetValue.Expanded,
                        enabledValues = PartiallyOrFullyExpanded
                    ),
                    snackbarHostState = viewModel.snackbar
                )
                NBTEditingHost(viewModel) {
                    val cutout = WindowInsets.systemBars.union(
                        WindowInsets.displayCutout
                    )
                    BottomSheetScaffold(
                        modifier = Modifier.fillMaxSize(),
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = with(LocalDensity.current) {
                            cutout.getBottom(this).toDp()
                        } + 36.dp,
                        sheetContent = {
                            val coroutineScope = rememberCoroutineScope()
                            TextButton("test") {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val db = info.storage.db
                                    val file = db.file(SpecialDBEntryType.LOCAL_PLAYER)
                                    if (file.isPresent()) {
                                        withContext(Dispatchers.Main) {
                                            viewModel.editing = file to NBTImportConfigImpl(
                                                NBTFormat.LITTLE_ENDIAN,
                                                HeaderPresence.UNCERTAIN
                                            )
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            viewModel.editing = LocalPlayerSource(
                                                info.world.config
                                            ) to NBTImportConfigImpl(
                                                NBTFormat.LITTLE_ENDIAN,
                                                HeaderPresence.PRESENT
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(with(LocalDensity.current) {
                                cutout.getBottom(this).toDp()
                            }))
                        }
                    ) { padding ->
                        MapUI(Modifier, state = viewModel.map)
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

            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
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
        targetState = viewModel.editing,
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
                viewModel.editing = null
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