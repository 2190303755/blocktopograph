package com.mithrilmania.blocktopograph.texture

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/** terrain_texture.json / item_texture.json; unknown fields are ignored. */
@Serializable
data class TextureAtlasJson(
    @SerialName("resource_pack_name") val resourcePackName: String = "",
    @SerialName("texture_name") val textureName: String = "",
    @SerialName("texture_data") val textureData: Map<String, TextureEntry> = emptyMap(),
)

/**
 * [textures] comes in three shapes: a path, a list of paths, or a list of
 * `{"path": ..., "overlay_color": ...}` objects.
 */
@Serializable
data class TextureEntry(
    val textures: JsonElement,
    @SerialName("overlay_color") val overlayColor: String? = null,
    @SerialName("tint_color") val tintColor: String? = null,
)

/** Only the first path is used, extension-less; tint / overlay_color are ignored for now. */
fun TextureEntry.basePath(): String? = when (val textures = textures) {
    is JsonPrimitive -> textures.content.takeIf { it.isNotBlank() }
    is JsonArray -> textures.firstOrNull()?.let { first ->
        when (first) {
            is JsonObject -> first["path"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            is JsonPrimitive -> first.content.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    else -> null
}

/** Official atlas files open with a `//` comment, and may grow new fields; parse forgivingly. */
val lenientTextureJson = Json {
    allowComments = true
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}
