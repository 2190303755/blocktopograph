package com.mithrilmania.blocktopograph.world

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
import android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
import android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.util.Size
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.IWorldCallback
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.storage.Location
import com.mithrilmania.blocktopograph.storage.ShizukuLocation
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.loadThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

class WorldListModel(app: Application) : AndroidViewModel(app) {
    val unscanned = Channel<Pair<String, Uri>>(capacity = Channel.UNLIMITED)
    val insertions = Channel<WorldDetail>(capacity = Channel.UNLIMITED)
    val snackbar: SnackbarHostState = SnackbarHostState()
    val registry: MutableMap<Location, WorldDetail> = ConcurrentHashMap()
    val statistics: MutableMap<Location, WorldStatistics> = ConcurrentHashMap()
    val worlds: SnapshotStateList<WorldDetail> = mutableStateListOf()
    var selected: WorldDetail? by mutableStateOf(null)
    var loading: Boolean by mutableStateOf(false)
    val callback: IWorldCallback = object : IWorldCallback.Stub() {
        override fun onWorldSubmit(
            path: String,
            config: ParcelFileDescriptor,
            icon: ParcelFileDescriptor?
        ) {
            viewModelScope.launch {
                loading = true
                val world = withContext(Dispatchers.IO) {
                    FileInputStream(config.fileDescriptor).extractDetail(
                        ShizukuLocation(path),
                        ShizukuLocation("$path/$FILE_LEVEL_DAT"),
                        application
                    )
                }
                if (world === null) {
                    loading = false
                    return@launch
                }
                insertions.send(world)
                loading = false
                if (icon === null) return@launch
                launch(Dispatchers.IO) {
                    world.icon = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        BitmapFactory.decodeStream(FileInputStream(icon.fileDescriptor))
                    } else icon.loadThumbnail(application.resources.let {
                        Size(
                            it.getDimensionPixelSize(R.dimen.large_world_icon_width),
                            it.getDimensionPixelSize(R.dimen.large_world_icon_height)
                        )
                    })
                }
            }
        }

        override fun onStatisticsUpdate(path: String, behaviors: Int, resources: Int, size: Long) {
            viewModelScope.launch {
                val location = ShizukuLocation(path)
                val inserted = registry[location]
                if (inserted === null) {
                    statistics[location] = WorldStatistics(behaviors, resources, size)
                } else {
                    inserted.behaviors = behaviors
                    inserted.resources = resources
                    inserted.size = ConvertUtil.formatSize(size)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            val projection = arrayOf(COLUMN_MIME_TYPE, COLUMN_LAST_MODIFIED, COLUMN_DOCUMENT_ID)
            for ((tag, folder) in unscanned) {
                loading = true
                withContext(Dispatchers.IO) {
                    val context = application
                    val resolver = context.contentResolver
                    resolver.query(
                        DocumentsContract.buildChildDocumentsUriUsingTree(
                            folder,
                            DocumentsContract.getDocumentId(folder)
                        ),
                        projection,
                        null,
                        null,
                        COLUMN_LAST_MODIFIED
                    )?.use { cursor ->
                        if (!cursor.moveToLast()) return@use // IDK why ` DESC` doesn't work, so reverse iteration
                        do {
                            if (cursor.isNull(0) || MIME_TYPE_DIR != cursor.getString(0)) continue
                            loadSAFWorld(
                                DocumentsContract.buildDocumentUriUsingTree(
                                    folder,
                                    cursor.getString(2)
                                ),
                                this,
                                tag,
                                context,
                                resolver
                            )
                        } while (cursor.moveToPrevious())
                    }
                }
                loading = false
            }
        }
        viewModelScope.launch {
            for (detail in insertions) {
                val location = detail.location
                if (registry.containsKey(location)) {
                    var remove = true
                    val iterator = worlds.listIterator()
                    while (iterator.hasNext()) {
                        if (location == iterator.next().location) {
                            iterator.set(detail)
                            registry[location] = detail
                            remove = false
                            break
                        }
                    }
                    if (remove) { // what happened?
                        registry.remove(location)
                    }
                } else {
                    registry[location] = detail
                    if (worlds.isEmpty()) {
                        worlds.add(detail)
                    } else {
                        var append = true
                        val iterator = worlds.listIterator()
                        while (iterator.hasNext()) {
                            val candidate = iterator.next()
                            if (detail.time > candidate.time) {
                                iterator.set(detail)
                                iterator.add(candidate)
                                append = false
                                break
                            }
                        }
                        if (append) {
                            iterator.add(detail)
                        }
                    }
                    statistics[location]?.let {
                        statistics.remove(location)
                        detail.behaviors = it.behaviors
                        detail.resources = it.resources
                        detail.size = ConvertUtil.formatSize(it.size)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        this.unscanned.close()
        this.insertions.close()
    }
}