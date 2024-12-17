package com.mithrilmania.blocktopograph.worldlist

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Size
import androidx.core.database.getLongOrNull
import androidx.lifecycle.MutableLiveData
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.getSize
import com.mithrilmania.blocktopograph.world.FILE_BEHAVIOR_PACKS
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.FILE_RESOURCE_PACKS
import com.mithrilmania.blocktopograph.world.FILE_WORLD_ICON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

suspend fun WorldItem.complete(adapter: WorldItemAdapter, context: Context) {
    withContext(Dispatchers.IO) {
        val insert = async(Dispatchers.Main) {
            adapter.notifyItemInserted(adapter.model.worlds.indexOf(this@complete))
        }
        val root = this@complete.location
        val resolver = context.contentResolver
        val bitmap = async(Dispatchers.IO) {
            val resources = context.resources
            root.findChild(resolver, FILE_WORLD_ICON)?.let { icon ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.loadThumbnail(
                        icon, Size(
                            resources.getDimension(R.dimen.large_world_icon_width).toInt(),
                            resources.getDimension(R.dimen.large_world_icon_height).toInt()
                        ), null
                    )
                } else resolver.openInputStream(icon)?.let { BitmapFactory.decodeStream(it) }
            }
        }
        val behavior = async(Dispatchers.IO) {
            root.findChild(resolver, FILE_BEHAVIOR_PACKS)?.let { packs ->
                resolver.openInputStream(packs)?.let {
                    BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8))
                }?.use {
                    JSONArray(it.readText()).length()
                }
            } ?: 0
        }
        val resource = async(Dispatchers.IO) {
            root.findChild(resolver, FILE_RESOURCE_PACKS)?.let { packs ->
                resolver.openInputStream(packs)?.let {
                    BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8))
                }?.use {
                    JSONArray(it.readText()).length()
                }
            } ?: 0
        }
        val size = async(Dispatchers.IO) { root.getSize(resolver) }
        this@complete.behavior = behavior.await()
        this@complete.resource = resource.await()
        this@complete.icon = bitmap.await()
        insert.await()
        withContext(Dispatchers.Main) {
            adapter.notifyItemChanged(adapter.model.worlds.indexOf(this@complete))
        }
        this@complete.size = ConvertUtil.formatSize(size.await())
        withContext(Dispatchers.Main) {
            val model = adapter.model
            adapter.notifyItemChanged(model.worlds.indexOf(this@complete))
            if (this@complete == model.selected.value) {
                model.selected.postValue(this@complete)
            }
        }
    }
}

suspend fun loadWorlds(
    adapter: WorldItemAdapter,
    context: Context,
    location: Uri,
    state: MutableLiveData<Boolean>,
    tag: String = ""
) {
    state.postValue(true)
    val worlds = adapter.model.worlds
    val resolver = context.contentResolver
    resolver.query(
        DocumentsContract.buildChildDocumentsUriUsingTree(
            location, DocumentsContract.getDocumentId(location)
        ), arrayOf(
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        ), null, null, DocumentsContract.Document.COLUMN_LAST_MODIFIED
    )?.use {
        if (!it.moveToLast()) return// idk why ` DESC` doesn't work, so reverse iteration
        forEachCandidate@ do {
            if (it.isNull(0) || DocumentsContract.Document.MIME_TYPE_DIR != it.getString(0)) continue
            val candidate = DocumentsContract.buildDocumentUriUsingTree(location, it.getString(2))
            val config: CompoundTag? = LevelDataConverter.read(
                resolver.openInputStream(
                    candidate.findChild(
                        resolver,
                        FILE_LEVEL_DAT
                    ) ?: continue
                )
            )
            val world = WorldItem(candidate, config, context, it.getLongOrNull(1) ?: 0, tag)
            var index = worlds.size
            while (--index >= 0) {
                if (world.time < worlds[index].time) {
                    if (world.location == worlds[index + 1].location) continue@forEachCandidate
                    worlds.add(index + 1, world)
                    world.complete(adapter, context)
                    continue@forEachCandidate
                }
            }
            worlds.add(0, world)
            world.complete(adapter, context)
        } while (it.moveToPrevious())
    }
    state.postValue(false)
}
