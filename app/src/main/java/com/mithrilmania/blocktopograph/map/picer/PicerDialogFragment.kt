package com.mithrilmania.blocktopograph.map.picer

import android.app.Dialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.core.graphics.createBitmap
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.google.android.material.snackbar.Snackbar
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.chunk.Chunk
import com.mithrilmania.blocktopograph.editor.world.WorldViewerModel
import com.mithrilmania.blocktopograph.map.edit.RectEditTarget
import com.mithrilmania.blocktopograph.ui.component.DialogFragmentLayout
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.world.WorldModel
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.defaultMapTypeCompat
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

interface PicerState {
    class Analyzing(@JvmField val job: Job) : PicerState
    class Analyzed(@JvmField val area: Rect, @JvmField val maxScale: Int) : PicerState
    object WorldOutOfSize : PicerState
    object SelectionOutOfSize : PicerState
    class Generating(@JvmField val job: Job) : PicerState
    object NoTerrain : PicerState
    class Failed(@JvmField @StringRes val message: Int) : PicerState
    class Generated(@JvmField val bitmap: Bitmap) : PicerState
    object Saving : PicerState
}


@Composable
fun LoadingIndicator() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator() }
}

fun WorldViewerModel.generateBitmap(
    area: Rect,
    scale: Int,
    storage: WorldStorage
): Job = viewModelScope.launch(Dispatchers.IO) {
    val dimension = this@generateBitmap.dimension
    val renderer = dimension.defaultMapTypeCompat().renderer

    val width = area.width() + 1
    val height = area.height() + 1
    val bitmap = createBitmap(width * scale, height * scale, Bitmap.Config.ARGB_8888)
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
    picerState = PicerState.Generated(bitmap)
}

fun DialogFragment.saveBitmap(bitmap: Bitmap, name: String) {
    val resolver = context?.contentResolver ?: return
    lifecycleScope.launch(Dispatchers.IO) {
        val spec = ContentValues()
        spec.put(MediaStore.Images.Media.DISPLAY_NAME, name)
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
                context?.toast(R.string.general_failed)
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            activity?.let { activity ->
                Snackbar.make(
                    activity.window.decorView,
                    activity.getString(R.string.picer_saved),
                    Snackbar.LENGTH_SHORT
                ).setAction(R.string.general_share) {
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.setType("image/png")
                    startActivity(
                        Intent.createChooser(
                            intent,
                            activity.getString(R.string.picer_share_title)
                        )
                    )
                }.show()
            }
            dismiss()
        }
    }
}

class PicerDialogFragment : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = ComposeView(this.requireContext())
        // Dispose of the Composition when the view's LifecycleOwner is destroyed
        root.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        root.setContent {
            BlocktopographCompatTheme {
                val owner =
                    LocalActivity.current as? ViewModelStoreOwner ?: this@PicerDialogFragment
                val viewer = viewModel<WorldViewerModel>(owner)
                val handle = viewModel<WorldModel>(owner)
                AnimatedContent(
                    viewer.picerState,
                    Modifier
                        .clip(AlertDialogDefaults.shape)
                        .background(MaterialTheme.colorScheme.surface),
                    transitionSpec = {
                        (fadeIn(
                            animationSpec = tween(220, delayMillis = 90)
                        ) + expandVertically()) togetherWith (fadeOut(
                            animationSpec = tween(90)
                        ) + shrinkVertically())
                    }
                ) { state ->
                    when (state) {
                        null -> {
                            Spacer(Modifier)
                            SideEffect { dismiss() }
                        }

                        PicerState.WorldOutOfSize -> DialogFragmentLayout(
                            stringResource(R.string.map_picer_world_too_large),
                            buttons = {
                                TextButton({
                                    dismiss()
                                    viewer.longPressCenter.trigger()
                                }) { Text(stringResource(android.R.string.ok)) }
                            }
                        ) { Text(stringResource(R.string.map_picer_use_selection_instead)) }

                        PicerState.SelectionOutOfSize -> DialogFragmentLayout(
                            stringResource(R.string.map_picer_selection_too_large),
                            buttons = {
                                TextButton({ dismiss() }) { Text(stringResource(android.R.string.ok)) }
                            }
                        ) {
                            Text(
                                stringResource(
                                    R.string.map_picer_selection_too_large_detail,
                                    MAX_LENGTH,
                                    MAX_AREA
                                )
                            )
                        }

                        PicerState.Saving -> DialogFragmentLayout(
                            stringResource(R.string.picer_save), // fixme: saving
                            content = ::LoadingIndicator
                        )

                        PicerState.NoTerrain, is PicerState.Generating -> DialogFragmentLayout(
                            stringResource(R.string.picer_progress_generating),
                        ) {
                            LoadingIndicator()
                            if (state === PicerState.NoTerrain) {
                                SideEffect(Unit) {
                                    dismiss()
                                    context?.toast(R.string.picer_failed_nodata)
                                }
                            }
                        }

                        is PicerState.Analyzing -> DialogFragmentLayout(
                            stringResource(R.string.picer_progress_analyzing),
                            content = ::LoadingIndicator
                        )

                        is PicerState.Analyzed -> {
                            val maxScale = state.maxScale.fastCoerceAtMost(MAX_SCALE)
                            val sliderState = rememberSliderState(
                                value = 1F,
                                steps = maxScale - 2,
                                trackRange = 1F..maxScale.toFloat()
                            )
                            DialogFragmentLayout(
                                stringResource(R.string.picer_title),
                                buttons = {
                                    TextButton({
                                        val storage = handle.world.storage
                                        viewer.picerState =
                                            if (storage === null) PicerState.NoTerrain
                                            else PicerState.Generating(
                                                viewer.generateBitmap(
                                                    state.area,
                                                    sliderState.value.roundToInt(),
                                                    storage
                                                )
                                            )
                                    }) { Text(stringResource(R.string.picer_btn_generate)) }
                                }
                            ) {
                                if (maxScale > 1) {
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
                                } else {
                                    Text(stringResource(R.string.picer_warn_not_scalable))
                                }
                            }
                        }

                        is PicerState.Failed -> DialogFragmentLayout(
                            stringResource(R.string.general_failed),
                            buttons = {
                                TextButton({
                                    dismiss()
                                }) { Text(stringResource(android.R.string.ok)) }
                            }
                        ) {
                            Text(stringResource(state.message))
                        }

                        is PicerState.Generated -> DialogFragmentLayout(
                            stringResource(R.string.picer_preview),
                            buttons = {
                                TextButton({
                                    viewer.picerState = PicerState.Saving
                                    saveBitmap(
                                        state.bitmap,
                                        ConvertUtil.getLegalFileName(handle.world.plainName) + "_map"
                                    )
                                }) { Text(stringResource(R.string.picer_save)) }
                            }
                        ) {
                            Column(Modifier.wrapContentHeight()) {
                                val bitmap = state.bitmap
                                AsyncImage(
                                    bitmap,
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
                }
            }
        }
        return root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return ComponentDialog(this.requireContext(), R.style.ComposeDialog)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        val state = ViewModelProvider(
            this.activity ?: return
        )[WorldViewerModel::class.java].picerState
        when (state) {
            is PicerState.Analyzing -> state.job.cancel()
            is PicerState.Generating -> state.job.cancel()
        }
    }

    companion object {
        const val MAX_LENGTH: Int = 2048
        const val MAX_AREA: Int = 64 * 64 * 256
        const val MAX_SCALE: Int = 32
    }
}
