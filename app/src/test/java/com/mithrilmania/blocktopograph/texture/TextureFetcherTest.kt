package com.mithrilmania.blocktopograph.texture

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TextureFetcherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun zipOf(vararg entries: Pair<String, ByteArray>): File {
        val file = tempFolder.newFile()
        ZipOutputStream(file.outputStream()).use { out ->
            for ((name, bytes) in entries) {
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }
        return file
    }

    private fun bytes(text: String): ByteArray = text.toByteArray(Charsets.UTF_8)

    @Test
    fun fetchReturnsInMemorySourceWithoutDiskCacheFile() = runBlocking {
        val atlas =
            """{"resource_pack_name":"vanilla","texture_data":{"stone":{"textures":"textures/blocks/stone"}}}"""
        val file = zipOf(
            TERRAIN_ATLAS_PATH to bytes(atlas),
            "textures/blocks/stone.png" to bytes("STONE_BYTES"),
        )
        val flow = MutableStateFlow(listOf(ResourcePack("vanilla", file.absolutePath, true)))
        val resolver = TextureResolver(flow) { File(it) }

        try {
            assertEquals(1, resolver.enabledZips.first { it.size == 1 }.size)
            val request = TextureRequest("textures/blocks/stone", 12L)
            val fetcher = resolver.create(request, Options(mock()), mock<ImageLoader>())
            assertTrue(fetcher is TextureFetcher)

            val result = fetcher.fetch() as SourceFetchResult
            assertEquals("STONE_BYTES", result.source.source().readUtf8())
            assertEquals("image/png", result.mimeType)
            assertEquals(DataSource.DISK, result.dataSource)
            // no diskCacheKey, and nothing hits the disk until file() is called
            assertNull(result.source.fileOrNull())
        } finally {
            resolver.close()
        }
    }

    @Test
    fun fetchReturnsNullOnMiss() = runBlocking {
        val resolver = TextureResolver(MutableStateFlow(emptyList<ResourcePack>())) { File(it) }
        try {
            assertTrue(resolver.enabledZips.first { it.isEmpty() }.isEmpty())
            assertNull(
                TextureFetcher(
                    resolver,
                    TextureRequest("textures/blocks/missing", 1L)
                ).fetch()
            )
        } finally {
            resolver.close()
        }
    }

    @Test
    fun keyerReadsOnlyRequestFields() {
        val options = Options(mock())
        val keyer = TextureKeyer()

        assertEquals(
            "textures/blocks/stone@v3",
            keyer.key(TextureRequest("textures/blocks/stone", 3L), options),
        )
        assertNotEquals(
            keyer.key(TextureRequest("textures/blocks/stone", 3L), options),
            keyer.key(TextureRequest("textures/blocks/stone", 4L), options),
        )
        assertNotEquals(
            keyer.key(TextureRequest("textures/blocks/stone", 3L), options),
            keyer.key(TextureRequest("textures/blocks/dirt", 3L), options),
        )
    }
}
