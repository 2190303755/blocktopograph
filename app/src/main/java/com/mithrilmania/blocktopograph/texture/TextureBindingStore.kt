package com.mithrilmania.blocktopograph.texture

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.mithrilmania.blocktopograph.util.jsonSerializer

/** typeId -> texture name. */
typealias TextureBinding = Map<String, String>

/** Aliases that cannot be guessed from the id alone; users may edit or drop them. */
val BUILTIN_BLOCK_TEXTURE_BINDINGS: TextureBinding = mapOf(
    "minecraft:brick_block" to "brick",
    "minecraft:lit_redstone_ore" to "redstone_ore",
)

/** Nothing to bridge on the item side yet. */
val BUILTIN_ITEM_TEXTURE_BINDINGS: TextureBinding = mapOf()

val Context.blockTextureBindingStore: DataStore<TextureBinding> by bindingStore(
    fileName = "block_texture_binding.json",
    builtin = BUILTIN_BLOCK_TEXTURE_BINDINGS,
)

val Context.itemTextureBindingStore: DataStore<TextureBinding> by bindingStore(
    fileName = "item_texture_binding.json",
    builtin = BUILTIN_ITEM_TEXTURE_BINDINGS,
)

/** Only writes the store; the lookup, version and cache all follow from the flow. */
suspend fun DataStore<TextureBinding>.setBinding(typeId: String, textureName: String) {
    updateData { it + (typeId to textureName) }
}

suspend fun DataStore<TextureBinding>.removeBinding(typeId: String) {
    updateData { it - typeId }
}

private fun bindingStore(
    fileName: String,
    builtin: TextureBinding
) = dataStore(
    fileName = fileName,
    serializer = jsonSerializer { builtin } // capture the value, and keep the serializer from being inlined away
)
