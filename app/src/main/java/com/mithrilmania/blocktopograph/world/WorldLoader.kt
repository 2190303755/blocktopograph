package com.mithrilmania.blocktopograph.world

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.application
import com.mithrilmania.blocktopograph.storage.SAFLocation
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.getIdOfDocumentOrTreeDocument
import com.mithrilmania.blocktopograph.util.getSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

suspend fun WorldListModel.loadSAFWorld(
    rootTree: Uri,
    coroutineScope: CoroutineScope,
    tag: String = "",
    context: Context = this.application,
    resolver: ContentResolver = context.contentResolver,
    rootId: String = rootTree.getIdOfDocumentOrTreeDocument(context)
) {
    val rootDocument = DocumentsContract.buildDocumentUriUsingTree(rootTree, rootId)
    val config = rootDocument.findChild(resolver, FILE_LEVEL_DAT) ?: return
    val world = resolver.openInputStream(config)?.extractDetail(
        SAFLocation(rootDocument),
        SAFLocation(config),
        context,
        tag
    ) ?: return
    coroutineScope.launch {
        val behavior = rootDocument.findChild(
            resolver,
            FILE_BEHAVIOR_PACKS
        )?.let { packs ->
            resolver.openInputStream(packs)?.let {
                BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8))
            }?.use {
                JSONArray(it.readText()).length()
            }
        } ?: 0
        withContext(Dispatchers.Main) {
            world.behaviors = behavior
        }
    }
    coroutineScope.launch {
        val resource = rootDocument.findChild(
            resolver,
            FILE_RESOURCE_PACKS
        )?.let { packs ->
            resolver.openInputStream(packs)?.let {
                BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8))
            }?.use {
                JSONArray(it.readText()).length()
            }
        } ?: 0
        withContext(Dispatchers.Main) {
            world.resources = resource
        }
    }
    coroutineScope.launch {
        val size = ConvertUtil.formatSize(rootDocument.getSize(resolver))
        withContext(Dispatchers.Main) {
            world.size = size
        }
    }
    insertions.send(world)
}