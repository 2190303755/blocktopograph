package com.mithrilmania.blocktopograph.editor.world

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.map.Biome
import com.mithrilmania.blocktopograph.util.APP_TAG
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

class FlatLayer(
    state: BlockTemplate = BlockTemplates.getOfType("minecraft:air").first(),
    height: Int = 1
) {
    @JvmField
    val uid: Int = UID.getAndIncrement()
    var state: BlockTemplate by mutableStateOf(state)
    var height: Int by mutableIntStateOf(height)

    companion object {
        @JvmStatic
        private val UID: AtomicInteger = AtomicInteger()
    }
}

fun FlatLayer.copy(): FlatLayer = FlatLayer(this.state, this.height)

const val KEY_BIOME_ID = "biome_id"
const val KEY_BLOCK_LAYERS = "block_layers"
const val KEY_BLOCK_NAME = "block_name"
const val KEY_BLOCK_DATA = "block_data"
const val KEY_COUNT = "count"
const val KEY_VERSION = "encoding_version"
const val KEY_STRUCTURE_OPS = "structure_options"

fun List<FlatLayer>.toJson(
    biome: Biome,
    version: Int = 4
): String? {
    try {
        val root = JSONObject()
        root.put(KEY_BIOME_ID, biome.id)
        root.put(KEY_VERSION, version)
        val layers = JSONArray()
        this.forEach {
            val layer = JSONObject()
            layer.put(KEY_BLOCK_NAME, it.state.block.name)
            layer.put(KEY_COUNT, it.height)
            layers.put(layer)
        }
        root.put(KEY_BLOCK_LAYERS, layers)
        return root.toString(4)
    } catch (e: JSONException) {
        Log.e(APP_TAG, "Failed to encode layers to JSON", e)
    }
    return null
}