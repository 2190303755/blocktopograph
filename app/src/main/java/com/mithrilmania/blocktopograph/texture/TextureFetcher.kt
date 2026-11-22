package com.mithrilmania.blocktopograph.texture

import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import okio.Buffer
import okio.FileSystem

/**
 * Only [TextureRequest.path] matters here; the version belongs to the key. Bytes stay in memory, no
 * disk cache is written, and null on a miss lets Coil fall back to the placeholder.
 */
class TextureFetcher(
    private val resolver: TextureResolver,
    private val request: TextureRequest,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val bytes = resolver.resolve(request.path) ?: return null
        return SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply { write(bytes) },
                fileSystem = fileSystem,
            ),
            mimeType = "image/png",
            dataSource = DataSource.DISK,
        )
    }
}
