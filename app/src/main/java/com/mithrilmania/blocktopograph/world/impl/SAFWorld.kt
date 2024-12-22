package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.getSize
import com.mithrilmania.blocktopograph.world.BUNDLE_ENTRY_NAME
import com.mithrilmania.blocktopograph.world.FILE_BEHAVIOR_PACKS
import com.mithrilmania.blocktopograph.world.FILE_RESOURCE_PACKS
import com.mithrilmania.blocktopograph.world.FILE_WORLD_ICON
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.worldlist.WorldItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class SAFWorld(
    location: Uri,
    name: String,
    mode: String,
    time: Long,
    seed: String,
    private val config: Uri,
    tag: String = ""
) : World<Uri>(
    location,
    name,
    mode,
    time,
    seed,
    tag,
    location.lastPathSegment ?: location.toString()
) {
    override fun prepareIntent(intent: Intent) = intent
        .setData(this.location)
        .putExtra(BUNDLE_ENTRY_NAME, this.name)

    suspend fun populate(adapter: WorldItemAdapter, context: Context) {
        withContext(Dispatchers.IO) {
            val insert = async(Dispatchers.Main) {
                adapter.notifyItemInserted(adapter.model.worlds.indexOf(this@SAFWorld))
            }
            val root = this@SAFWorld.location
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
            async(Dispatchers.Default) {
                this@SAFWorld.behavior = behavior.await()
                this@SAFWorld.resource = resource.await()
                this@SAFWorld.icon = bitmap.await()
                insert.await()
                withContext(Dispatchers.Main) {
                    adapter.notifyItemChanged(this@SAFWorld)
                }
                this@SAFWorld.size = ConvertUtil.formatSize(size.await())
                withContext(Dispatchers.Main) {
                    adapter.notifyItemChanged(this@SAFWorld)
                }
            }
        }
    }
}