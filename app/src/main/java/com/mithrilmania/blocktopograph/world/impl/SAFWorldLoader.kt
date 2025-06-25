package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Size
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.storage.SAFLocation
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.getSize
import com.mithrilmania.blocktopograph.world.FILE_BEHAVIOR_PACKS
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.FILE_RESOURCE_PACKS
import com.mithrilmania.blocktopograph.world.FILE_WORLD_ICON
import com.mithrilmania.blocktopograph.world.WorldInfo
import com.mithrilmania.blocktopograph.world.extractInfo
import com.mithrilmania.blocktopograph.worldlist.WorldItemAdapter
import com.mithrilmania.blocktopograph.worldlist.WorldListModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

suspend fun Uri.populate(info: WorldInfo, adapter: WorldItemAdapter, context: Context) {
    val root = this
    withContext(Dispatchers.IO) {
        val insert = async(Dispatchers.Main) {
            adapter.worlds.add(info)
        }
        val resolver = context.contentResolver
        val bitmap = async(Dispatchers.IO) {
            val resources = context.resources
            root.findChild(resolver, FILE_WORLD_ICON)?.let { icon ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.loadThumbnail(
                        icon, Size(
                            resources.getDimensionPixelSize(R.dimen.large_world_icon_width),
                            resources.getDimensionPixelSize(R.dimen.large_world_icon_height)
                        ), null
                    )
                } else resolver.openInputStream(icon)
                    ?.let { BitmapFactory.decodeStream(it) }
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
        launch(Dispatchers.Default) {
            info.behavior = behavior.await()
            info.resource = resource.await()
            info.icon = bitmap.await()
            insert.await()
            withContext(Dispatchers.Main) {
                adapter.notifyItemChanged(info)
            }
            info.size = ConvertUtil.formatSize(size.await())
            withContext(Dispatchers.Main) {
                adapter.notifyItemChanged(info)
            }
        }
    }
}

suspend fun loadSAFWorlds(
    model: WorldListModel,
    context: Context,
    location: Uri,
    tag: String
) {
    val adapter = model.adapter
    model.loading.postValue(true)
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
        if (!it.moveToLast()) return@use// idk why ` DESC` doesn't work, so reverse iteration
        forEachCandidate@ do {
            if (it.isNull(0) || DocumentsContract.Document.MIME_TYPE_DIR != it.getString(0)) continue
            val candidate =
                DocumentsContract.buildDocumentUriUsingTree(location, it.getString(2))
            val config = candidate.findChild(resolver, FILE_LEVEL_DAT) ?: continue
            val world = resolver.openInputStream(config)?.extractInfo(
                SAFLocation(candidate),
                SAFLocation(config),
                context,
                tag
            ) ?: continue
            candidate.populate(world, adapter, context)
        } while (it.moveToPrevious())
    }
    model.loading.postValue(false)
}