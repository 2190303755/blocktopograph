package com.mithrilmania.blocktopograph.map.picer

import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ScrollView
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.core.graphics.createBitmap
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.google.android.material.snackbar.Snackbar
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.chunk.Chunk
import com.mithrilmania.blocktopograph.chunk.Version
import com.mithrilmania.blocktopograph.databinding.FragPicerBinding
import com.mithrilmania.blocktopograph.map.edit.RectEditTarget
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.UiUtil
import com.mithrilmania.blocktopograph.util.readIntLE
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.chunk.ChunkTag
import com.mithrilmania.blocktopograph.world.defaultMapTypeCompat
import com.mithrilmania.blocktopograph.world.isOverworld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import org.iq80.leveldb.ReadOptions

suspend fun PicerFragment.analyzeChunksImpl(): Rect {
    val storage = this.mWorld.storage ?: throw NullPointerException()
    val db = storage.db
    val dimension = this.mDimension
    val versionMark = ChunkTag.VERSION.dataID
    val legacyVersionMark = ChunkTag.LEGACY_VERSION.dataID
    var rect: Rect? = null
    val extended = !dimension.isOverworld
    var hasWrongChunks = false
    var hasOldChunks = false
    db.iterator(ReadOptions().fillCache(false)).use { iterator ->
        iterator.seekToFirst()
        var count = 0
        while (iterator.hasNext()) {
            ++count
            if (!currentCoroutineContext().isActive) throw InterruptedException()
            val entry = iterator.next()
            val key = entry.key
            if (key.size != if (extended) 13 else 9) continue
            val mark = key.last()
            if (mark != versionMark && mark != legacyVersionMark) continue
            if (extended && key.readIntLE(8) != dimension.runtimeId) continue
            when (Version.getVersion(entry.value)) {
                Version.ERROR, Version.NULL -> hasWrongChunks = true
                Version.OLD_LIMITED -> hasOldChunks = true
                else -> {
                    val chunkX = key.readIntLE(0)
                    val chunkZ = key.readIntLE(4)
                    if (rect === null) {
                        rect = Rect(chunkX, chunkZ, chunkX, chunkZ)
                    } else {
                        rect.union(chunkX, chunkZ)
                        if (((count and 0x1F) == 0 && rect.width() * rect.height() > PicerFragment.MAX_AREA)
                            || rect.width() > PicerFragment.MAX_LENGTH
                            || rect.height() > PicerFragment.MAX_LENGTH
                        ) break
                    }
                }
            }
        }
    }
    if (rect === null) {
        if (hasWrongChunks) throw IllegalStateException()
        if (hasOldChunks) throw UnsupportedOperationException()
        throw NullPointerException()
    }
    rect.left *= 16
    rect.top *= 16
    rect.right *= 16
    rect.bottom *= 16
    return rect
}

fun analyzeChunks(fragment: PicerFragment) {
    val context = fragment.context
    if (context === null) {
        fragment.dismiss()
        return
    }
    var job: Job? = null
    val dialog = UiUtil.buildProgressWaitDialog(
        context,
        R.string.picer_progress_analyzing
    ) {
        job?.cancel()
        fragment.dismiss()
    }
    // TODO consider viewModelScope
    job = fragment.lifecycleScope.launch(Dispatchers.Default) {
        runCatching {
            fragment.analyzeChunksImpl()
        }.fold({
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                fragment.onAnalyzeDone(it)
            }
        }) {
            val message = when (it) {
                is InterruptedException -> throw it
                is IllegalStateException -> R.string.picer_failed_corrupt
                is UnsupportedOperationException -> R.string.picer_failed_old
                else -> {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        fragment.showFailureMsgAndDismiss(R.string.picer_failed_nodata)
                    }
                    return@launch
                }
            }
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                fragment.showFailureDialogAndDismiss(message)
            }
        }
    }
    dialog.show()
}

fun generateBitmap(
    fragment: PicerFragment,
    area: Rect,
    scale: Int,
    storage: WorldStorage,
    dialog: DialogInterface
): Job = fragment.lifecycleScope.launch(Dispatchers.Default) {
    // TODO consider viewModelScope
    val dimension = fragment.mDimension
    val renderer = dimension.defaultMapTypeCompat().renderer

    val width = area.right - area.left + 1
    val height = area.bottom - area.top + 1
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
    withContext(Dispatchers.Main) {
        fragment.onGenerationDone(bitmap, dialog)
    }
}

fun loadBitmap(binding: FragPicerBinding, bitmap: Bitmap) {
    binding.image.load(bitmap) {
        listener(
            onSuccess = { _, _ ->
                binding.scroll.post {
                    binding.scroll.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        )
    }
}

fun saveBitmap(fragment: DialogFragment, bitmap: Bitmap, name: String) {
    val resolver = fragment.context?.contentResolver ?: return
    // TODO consider viewModelScope
    fragment.lifecycleScope.launch {
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
                fragment.context?.toast(R.string.general_failed)
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            fragment.activity?.let { activity ->
                Snackbar.make(
                    activity.window.decorView,
                    activity.getString(R.string.picer_saved),
                    Snackbar.LENGTH_SHORT
                ).setAction(R.string.general_share) {
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.setType("image/png")
                    activity.startActivity(
                        Intent.createChooser(
                            intent,
                            activity.getString(R.string.picer_share_title)
                        )
                    )
                }.show()
            }
            fragment.dismiss()
        }
    }
}
