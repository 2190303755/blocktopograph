package com.mithrilmania.blocktopograph.world

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.application
import com.mithrilmania.blocktopograph.storage.SAFLocation
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.findChild
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
    root: Uri,
    coroutineScope: CoroutineScope,
    tag: String = "",
    context: Context = this.application,
    resolver: ContentResolver = context.contentResolver
) {
    val config = root.findChild(resolver, FILE_LEVEL_DAT) ?: return
    val world = resolver.openInputStream(config)?.extractDetail(
        SAFLocation(root),
        SAFLocation(config),
        context,
        root.findChild(resolver, FILE_WORLD_ICON)?.let { SAFLocation(it) },
        tag
    ) ?: return
    coroutineScope.launch {
        val behavior = root.findChild(
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
        val resource = root.findChild(
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
        val size = ConvertUtil.formatSize(root.getSize(resolver))
        withContext(Dispatchers.Main) {
            world.size = size
        }
    }
    insertions.send(world)
}