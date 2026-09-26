package com.mithrilmania.blocktopograph.map.picer

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.core.graphics.createBitmap
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.chunk.Chunk
import com.mithrilmania.blocktopograph.editor.world.WorldViewerModel
import com.mithrilmania.blocktopograph.map.edit.RectEditTarget
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.showSnackbar
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.world.WorldModel
import com.mithrilmania.blocktopograph.world.defaultMapTypeCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import kotlin.math.round
import kotlin.math.roundToInt

const val PICER_MAX_LENGTH: Int = 2048
const val PICER_MAX_AREA: Int = 64 * 64 * 256
const val PICER_MAX_SCALE: Int = 32

sealed interface PicerState {
    @get:StringRes
    val title: Int

    @get:StringRes
    val confirm: Int get() = android.R.string.ok

    fun onConfirm(
        viewer: WorldViewerModel,
        handle: WorldModel,
        viewScope: CoroutineScope
    )

    sealed interface CancelableState : PicerState
    sealed interface LoadingState : PicerState {
        override fun onConfirm(
            viewer: WorldViewerModel,
            handle: WorldModel,
            viewScope: CoroutineScope
        ) {
        }
    }

    class Analyzing(@JvmField val job: Job) : LoadingState {
        override val title: Int get() = R.string.picer_progress_analyzing
    }

    class Analyzed(@JvmField val area: IntRect, maxScale: Int) : CancelableState {
        override val title: Int get() = R.string.picer_title
        override val confirm: Int get() = R.string.picer_btn_generate

        @JvmField
        val sliderState: SliderState?

        init {
            val effectiveMaxScale = maxScale.fastCoerceAtMost(PICER_MAX_SCALE)
            this.sliderState = if (effectiveMaxScale < 2) null else SliderState(
                value = 1F,
                steps = effectiveMaxScale - 2,
                trackRange = 1F..effectiveMaxScale.toFloat()
            )
        }

        override fun onConfirm(
            viewer: WorldViewerModel,
            handle: WorldModel,
            viewScope: CoroutineScope
        ) {
            val storage = handle.world.storage
            if (storage === null) {
                viewer.picerState = null
                viewer.application.toast(R.string.picer_failed_nodata)
            } else {
                viewer.picerState = Generating(
                    viewer.viewModelScope.launch(Dispatchers.IO) {
                        val dimension = viewer.dimension
                        val renderer = dimension.defaultMapTypeCompat().renderer

                        val scale = sliderState?.value?.roundToInt() ?: 1
                        val width = area.width
                        val height = area.height
                        val bitmap =
                            createBitmap(width * scale, height * scale, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        val channel = Channel<Chunk>(Channel.UNLIMITED)
                        val workers = Runtime.getRuntime().availableProcessors() - 1
                        repeat(workers.fastCoerceAtLeast(1)) {
                            launch {
                                val paint = Paint()
                                for (chunk in channel) {
                                    renderer.renderToBitmap(
                                        chunk, canvas, dimension, chunk.mChunkX, chunk.mChunkZ,
                                        (chunk.mChunkX * 16 - area.left) * scale,
                                        (chunk.mChunkZ * 16 - area.top) * scale,
                                        scale, scale, paint, storage
                                    )
                                }
                            }
                        }
                        RectEditTarget(
                            storage,
                            area,
                            dimension
                        ).forEachChunk { chunk, _, _, _, _, _, _ ->
                            if (isActive) {
                                channel.trySend(chunk)
                            }
                            0
                        }
                        channel.close()
                        coroutineContext.job.children.forEach { it.join() }
                        viewer.picerState = Generated(bitmap)
                    }
                )
            }
        }
    }

    object WorldOutOfSize : PicerState {
        override val title: Int get() = R.string.map_picer_world_too_large
        override fun onConfirm(
            viewer: WorldViewerModel,
            handle: WorldModel,
            viewScope: CoroutineScope
        ) {
            viewer.picerState = null
            viewScope.launch {
                viewer.longPressCenter.emit(Unit)
            }
        }
    }

    object SelectionOutOfSize : PicerState {
        override val title: Int get() = R.string.map_picer_selection_too_large
        override fun onConfirm(
            viewer: WorldViewerModel,
            handle: WorldModel,
            viewScope: CoroutineScope
        ) {
            viewer.picerState = null
        }
    }

    class Generating(@JvmField val job: Job) : LoadingState {
        override val title: Int get() = R.string.picer_progress_generating
    }

    class Failed(@JvmField @StringRes val message: Int) : PicerState {
        override val title: Int get() = R.string.general_failed // fixme: specialize

        override fun onConfirm(
            viewer: WorldViewerModel,
            handle: WorldModel,
            viewScope: CoroutineScope
        ) {
            viewer.picerState = null
        }
    }

    class Generated(@JvmField val bitmap: Bitmap) : CancelableState {
        override val title: Int get() = R.string.picer_preview
        override fun onConfirm(
            viewer: WorldViewerModel,
            handle: WorldModel,
            viewScope: CoroutineScope
        ) {
            viewer.picerState = Saving
            viewScope.launch(Dispatchers.IO) {
                val resolver = viewer.application.contentResolver
                val spec = ContentValues()
                spec.put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    ConvertUtil.getLegalFileName(handle.world.plainName) + "_map"
                )
                spec.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                spec.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    spec.put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                var uri: Uri
                try {
                    uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, spec)
                        ?: throw IOException("Failed to create file")

                    resolver.openOutputStream(uri)?.use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 0, it)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        spec.clear()
                        spec.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, spec, null, null)
                    }
                } catch (e: Exception) {
                    Log.e(APP_TAG, "Error when saving bitmap", e)
                    withContext(Dispatchers.Main) {
                        viewer.application.toast(R.string.general_failed)
                    }
                    return@launch
                }

                viewer.viewModelScope.launch {
                    val resources = viewer.application.resources
                    viewer.snackbar.showSnackbar(
                        resources.getString(R.string.picer_saved),
                        resources.getString(R.string.general_share),
                        SnackbarDuration.Long
                    ) {
                        viewer.pendingIntent.send(
                            Intent.createChooser(
                                Intent().apply {
                                    setAction(Intent.ACTION_SEND)
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    setType("image/png")
                                },
                                resources.getString(R.string.picer_share_title)
                            )
                        )
                    }
                }
                viewer.picerState = null
            }
        }
    }

    object Saving : LoadingState {
        override val title: Int get() = R.string.picer_save // fixme: saving
    }
}

@Composable
fun PicerDialog(
    viewer: WorldViewerModel,
    handle: WorldModel
) {
    val onDismissRequest: () -> Unit = {
        when (val state = viewer.picerState) {
            is PicerState.Analyzing -> state.job.cancel()
            is PicerState.Generating -> state.job.cancel()
            else -> {}
        }
        viewer.picerState = null
    }
    viewer.picerState?.let { uiState ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(uiState.title)) },
            text = {
                when (uiState) {
                    PicerState.SelectionOutOfSize -> {
                        Text(
                            stringResource(
                                R.string.map_picer_selection_too_large_detail,
                                PICER_MAX_LENGTH,
                                PICER_MAX_AREA
                            )
                        )
                    }

                    PicerState.WorldOutOfSize -> {
                        Text(stringResource(R.string.map_picer_use_selection_instead))
                    }

                    is PicerState.LoadingState -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is PicerState.Analyzed -> {
                        val sliderState = uiState.sliderState
                        if (sliderState === null) {
                            Text(stringResource(R.string.picer_warn_not_scalable))
                        } else {
                            Column {
                                Text(stringResource(R.string.picer_scale_title))
                                Slider(sliderState, onValueChangeFinished = {
                                    sliderState.value = round(sliderState.value)
                                })
                                Text(
                                    sliderState.value.roundToInt().toString(),
                                    Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Text(stringResource(R.string.map_picer_scale_guide))
                            }
                        }
                    }

                    is PicerState.Failed -> {
                        Text(stringResource(uiState.message))
                    }

                    is PicerState.Generated -> {
                        Column {
                            AsyncImage(
                                uiState.bitmap,
                                null,
                                Modifier
                                    .weight(1F, false)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            )
                            Text(stringResource(R.string.picer_save_text)) // FIXME correct it
                        }
                    }
                }
            },
            dismissButton = {
                if (uiState is PicerState.CancelableState) {
                    TextButton(
                        stringResource(android.R.string.cancel),
                        onClick = onDismissRequest
                    )
                }
            },
            confirmButton = {
                val coroutineScope = rememberCoroutineScope()
                TextButton(
                    stringResource(uiState.confirm),
                    uiState !is PicerState.LoadingState
                ) {
                    viewer.picerState?.onConfirm(viewer, handle, coroutineScope)
                }
            }
        )
    }
}
