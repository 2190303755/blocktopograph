package com.mithrilmania.blocktopograph.texture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TextureResolverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun zip(name: String, vararg entries: Pair<String, String>): File {
        val file = tempFolder.newFile(name)
        ZipOutputStream(file.outputStream()).use { out ->
            for ((path, content) in entries) {
                out.putNextEntry(ZipEntry(path))
                out.write(content.toByteArray(Charsets.UTF_8))
                out.closeEntry()
            }
        }
        return file
    }

    @Test
    fun readsByPackPriorityAndExtensionAfterStackChange() = runBlocking {
        val high = zip("high.zip", "textures/blocks/stone.png" to "HIGH")
        val low = zip(
            "low.zip",
            "textures/blocks/stone.png" to "LOW",
            "textures/blocks/brick.jpeg" to "JPEG",
        )
        val flow = MutableStateFlow<List<ResourcePack>>(emptyList())
        val resolver = TextureResolver(flow) { File(it) }

        try {
            flow.value = listOf(
                ResourcePack("high", high.absolutePath, true),
                ResourcePack("low", low.absolutePath, true),
            )
            assertEquals(2, resolver.enabledZips.first { it.size == 2 }.size)
            assertEquals("HIGH", resolver.resolve("textures/blocks/stone")?.decodeToString())
            assertEquals("JPEG", resolver.resolve("textures/blocks/brick")?.decodeToString())
            assertNull(resolver.resolve("textures/blocks/missing"))

            // reordered: the same path now reads from another pack, which is what version exists to catch
            flow.value = listOf(
                ResourcePack("low", low.absolutePath, true),
                ResourcePack("high", high.absolutePath, true),
            )
            resolver.enabledZips.first { it.size == 2 }
            assertEquals("LOW", resolver.resolve("textures/blocks/stone")?.decodeToString())
        } finally {
            resolver.close()
        }
    }

    @Test
    fun disabledPacksAreClosedButNotDeleted() = runBlocking {
        val high = zip("high.zip", "textures/blocks/stone.png" to "HIGH")
        val low = zip("low.zip", "textures/blocks/stone.png" to "LOW")
        val flow = MutableStateFlow(
            listOf(
                ResourcePack("high", high.absolutePath, true),
                ResourcePack("low", low.absolutePath, false),
            ),
        )
        val resolver = TextureResolver(flow) { File(it) }

        try {
            assertEquals(1, resolver.enabledZips.first { it.size == 1 }.size)
            assertEquals("HIGH", resolver.resolve("textures/blocks/stone")?.decodeToString())

            flow.value = listOf(
                ResourcePack("high", high.absolutePath, false),
                ResourcePack("low", low.absolutePath, true),
            )
            assertEquals(1, resolver.enabledZips.first { it.size == 1 }.size)
            assertEquals("LOW", resolver.resolve("textures/blocks/stone")?.decodeToString())
            assertTrue(high.exists())
            assertTrue(low.exists())
        } finally {
            resolver.close()
        }
    }

    @Test
    fun removedPackIsClosedAndItsFileDeleted() = runBlocking {
        val file = zip("removed.zip", "textures/items/apple.png" to "APPLE")
        val flow = MutableStateFlow(
            listOf(ResourcePack("removed", file.absolutePath, true)),
        )
        val resolver = TextureResolver(flow) { File(it) }

        assertEquals(1, resolver.enabledZips.first { it.size == 1 }.size)
        assertEquals("APPLE", resolver.resolve("textures/items/apple")?.decodeToString())

        flow.value = emptyList()
        assertTrue(resolver.enabledZips.first { it.isEmpty() }.isEmpty())
        assertFalse(file.exists())
        assertNull(resolver.resolve("textures/items/apple"))
    }
}
