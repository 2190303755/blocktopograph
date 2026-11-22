package com.mithrilmania.blocktopograph.texture

import android.content.Context
import coil3.ImageLoader
import coil3.fetch.Fetcher
import coil3.request.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

private val TEXTURE_FILE_EXTENSIONS = listOf("png", "jpg", "jpeg")

/**
 * Turns a texture path into bytes. The pack list is its only input: no bindings, no lookup, no version.
 * The enabled zip handles are the one piece of mutable state here, and [reconcileZips] owns their
 * whole life cycle.
 */
class TextureResolver internal constructor(
    packsFlow: Flow<List<ResourcePack>>,
    private val zipFile: (String) -> File,
) : Fetcher.Factory<TextureRequest> {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val openZips = ConcurrentHashMap<String, ZipFile>()

    constructor(context: Context) : this(
        context.applicationContext.resourcePackStore.data,
        { path ->
            File(path).let { file ->
                if (file.isAbsolute) file else File(
                    context.applicationContext.filesDir,
                    path
                )
            }
        },
    )

    /**
     * Replayed so late subscribers still see the current stack, but with no initial value: [resolve]
     * waits for the first real emission instead of mistaking "nothing loaded yet" for "no textures".
     */
    val enabledZips: SharedFlow<List<ZipFile>> = packsFlow
        .distinctUntilChanged()
        .map { packs -> reconcileZips(packs) }
        .flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    /** Walks the packs in priority order, trying png / jpg / jpeg; null if none of them has the file. */
    suspend fun resolve(path: String): ByteArray? {
        val zips = enabledZips.first()
        for (zip in zips) {
            for (extension in TEXTURE_FILE_EXTENSIONS) {
                // A read already in flight may hit a handle reconcile just closed; count it as a miss.
                val entry = runCatching { zip.getEntry("$path.$extension") }.getOrNull() ?: continue
                runCatching { zip.getInputStream(entry).use { it.readBytes() } }.getOrNull()
                    ?.let { return it }
            }
        }
        return null
    }

    /** For process exit and tests; otherwise [reconcileZips] keeps handles in step with the flow. */
    internal fun close() {
        openZips.values.forEach { runCatching { it.close() } }
        openZips.clear()
    }

    private fun reconcileZips(packs: List<ResourcePack>): List<ZipFile> {
        val byPath = packs.associateBy { it.zipPath }
        // 1) Gone from the store: close it and delete the file
        openZips.keys.filterNot { it in byPath }.forEach { path ->
            openZips.remove(path)?.let { runCatching { it.close() } }
            runCatching { zipFile(path).delete() }
        }
        // 2) Disabled: close it, but keep the file around
        openZips.keys.filter { byPath[it]?.enabled == false }.forEach { path ->
            openZips.remove(path)?.let { runCatching { it.close() } }
        }
        // 3) Enabled but not open yet: open it
        for (pack in packs) {
            if (pack.enabled && !openZips.containsKey(pack.zipPath)) {
                runCatching { ZipFile(zipFile(pack.zipPath)) }.getOrNull()?.let {
                    openZips[pack.zipPath] = it
                }
            }
        }
        // 4) Return the enabled zips in priority order
        return packs.filter { it.enabled }.mapNotNull { openZips[it.zipPath] }
    }

    override fun create(
        data: TextureRequest,
        options: Options,
        imageLoader: ImageLoader
    ): Fetcher = TextureFetcher(this, data, options.fileSystem)
}
