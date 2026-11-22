package com.mithrilmania.blocktopograph.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.texture.BUILTIN_BLOCK_TEXTURE_BINDINGS
import com.mithrilmania.blocktopograph.texture.BUILTIN_ITEM_TEXTURE_BINDINGS
import com.mithrilmania.blocktopograph.texture.ResourcePack
import com.mithrilmania.blocktopograph.texture.ResourcePackImporter
import com.mithrilmania.blocktopograph.texture.TextureLookup
import com.mithrilmania.blocktopograph.texture.TextureRequest
import com.mithrilmania.blocktopograph.texture.TextureRequestFactory
import com.mithrilmania.blocktopograph.texture.ZIP_BUFFER_SIZE
import com.mithrilmania.blocktopograph.texture.blockTextureBindingStore
import com.mithrilmania.blocktopograph.texture.buildTextureLookupFromPacks
import com.mithrilmania.blocktopograph.texture.itemTextureBindingStore
import com.mithrilmania.blocktopograph.texture.removeBinding
import com.mithrilmania.blocktopograph.texture.resourcePackStore
import com.mithrilmania.blocktopograph.texture.setBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Activity-scoped. Everything here is derived from a DataStore, so UI actions only write and the rest
 * (lookup, version, cache) takes care of itself.
 */
class ResourcePackViewModel(app: Application) : AndroidViewModel(app) {

    /** Rebuilt from scratch whenever the enabled stack changes. */
    val lookup: StateFlow<TextureLookup> = app.resourcePackStore.data
        .map { it.filter { pack -> pack.enabled } }
        .distinctUntilChanged()
        .map { packs -> buildTextureLookupFromPacks(packs, app.filesDir) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, TextureLookup.EMPTY)

    /** What the UI collects; any change re-derives the requests, which changes the cache keys. */
    val factory: StateFlow<TextureRequestFactory> = combine(
        lookup,
        app.blockTextureBindingStore.data,
        app.itemTextureBindingStore.data,
    ) { lk, blockBindings, itemBindings ->
        TextureRequestFactory(lk, blockBindings, itemBindings)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TextureRequestFactory.EMPTY)

    val packs: StateFlow<List<ResourcePack>> = app.resourcePackStore.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val blockBindings: StateFlow<Map<String, String>> = app.blockTextureBindingStore.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, BUILTIN_BLOCK_TEXTURE_BINDINGS)

    val itemBindings: StateFlow<Map<String, String>> = app.itemTextureBindingStore.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, BUILTIN_ITEM_TEXTURE_BINDINGS)

    /** Copies through SAF, slims it down, writes the metadata; the resolver opens it on the next reconcile. */
    suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        val context = application
        val temp = File(context.cacheDir, "resource_pack_import_${System.nanoTime()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output, ZIP_BUFFER_SIZE) }
            } ?: throw IOException("Cannot read the selected file")

            val imported = ZipFile(temp).use { source ->
                ResourcePackImporter.locatePacks(source).map { discovered ->
                    val id = ResourcePackImporter.packId(discovered, temp.nameWithoutExtension)
                    val zipPath = "resource_packs/$id.zip"
                    ResourcePackImporter.repack(source, discovered, File(context.filesDir, zipPath))
                    ResourcePack(id = id, zipPath = zipPath, enabled = true)
                }
            }
            context.resourcePackStore.updateData { packs ->
                val ids = imported.map { it.id }.toSet()
                packs.filterNot { it.id in ids } + imported
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        application.resourcePackStore.updateData { packs ->
            packs.map {
                if (it.id == id) it.copy(enabled = enabled) else it
            }
        }
    }

    /** Metadata only; reconcileZips closes and deletes the file. */
    suspend fun remove(id: String) {
        application.resourcePackStore.updateData { packs ->
            packs.filterNot { it.id == id }
        }
    }

    /** [ids] is the new order; anything not listed sinks to the end. */
    suspend fun reorder(ids: List<String>) {
        val rank = ids.withIndex().associate { it.value to it.index }
        application.resourcePackStore.updateData { packs ->
            packs.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
        }
    }

    suspend fun setBlockBinding(typeId: String, textureName: String) =
        application.blockTextureBindingStore.setBinding(typeId, textureName)

    suspend fun removeBlockBinding(typeId: String) =
        application.blockTextureBindingStore.removeBinding(typeId)

    suspend fun setItemBinding(typeId: String, textureName: String) =
        application.itemTextureBindingStore.setBinding(typeId, textureName)

    suspend fun removeItemBinding(typeId: String) =
        application.itemTextureBindingStore.removeBinding(typeId)
}

/**
 * One per Activity. Every NavEntry has its own ViewModelStore, so calling viewModel() from a screen
 * would hand out several copies and rebuild the lookup each time.
 */
val LocalResourcePackViewModel = staticCompositionLocalOf<ResourcePackViewModel> {
    error("ResourcePackViewModel was not provided by the host Activity")
}

/** Watches the factory, so a changed lookup or binding produces a new request and Coil reloads. */
@Composable
fun rememberBlockTextureRequest(typeId: String): TextureRequest? =
    rememberTextureRequest(typeId) { it.createBlockRequest(typeId) }

@Composable
fun rememberItemTextureRequest(typeId: String): TextureRequest? =
    rememberTextureRequest(typeId) { it.createItemRequest(typeId) }

@Composable
private fun rememberTextureRequest(
    typeId: String,
    create: (TextureRequestFactory) -> TextureRequest?,
): TextureRequest? {
    val vm = LocalResourcePackViewModel.current
    val factory by vm.factory.collectAsStateWithLifecycle()
    return remember(typeId, factory) { create(factory) }
}
