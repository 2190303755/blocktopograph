package com.mithrilmania.blocktopograph.texture

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ResourcePackImporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ZipFile {
        val file = tempFolder.newFile()
        ZipOutputStream(file.outputStream()).use { out ->
            for ((name, bytes) in entries) {
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }
        return ZipFile(file)
    }

    private fun manifest(uuid: String?, type: String = "resources"): ByteArray = buildString {
        append("""{"header": {""")
        if (uuid != null) append(""""uuid": "$uuid"""")
        append("""}, "modules": [{"type": "$type"}]}""")
    }.toByteArray(Charsets.UTF_8)

    private fun ZipFile.entryNames(): Set<String> = Collections.list(entries()).map { it.name }.toSet()

    @Test
    fun locatesFirstLevelResourcePacksAndRepacksSlimZip() {
        zipOf(
            "behavior_pack/manifest.json" to manifest("bp", type = "data"),
            "resource_pack/manifest.json" to manifest("res-1"),
            "resource_pack/pack_icon.png" to "ICON".toByteArray(),
            "resource_pack/textures/terrain_texture.json" to "{}".toByteArray(),
            "resource_pack/textures/blocks/stone.png" to "STONE".toByteArray(),
            "resource_pack/sounds/click.ogg" to "SOUND".toByteArray(),
        ).use { source ->
            val discovered = ResourcePackImporter.locatePacks(source)
            assertEquals(1, discovered.size)
            assertEquals("resource_pack", discovered[0].root)
            assertEquals("res-1", discovered[0].uuid)
            assertEquals("res-1", ResourcePackImporter.packId(discovered[0], "fallback"))

            val target = File(tempFolder.root, "res-1.zip")
            ResourcePackImporter.repack(source, discovered[0], target)

            ZipFile(target).use { result ->
                assertEquals(
                    setOf("manifest.json", "pack_icon.png", "textures/terrain_texture.json", "textures/blocks/stone.png"),
                    result.entryNames(),
                )
                assertEquals(
                    "STONE",
                    result.getInputStream(result.getEntry("textures/blocks/stone.png")).use {
                        it.readBytes().decodeToString()
                    },
                )
                assertTrue(result.getEntry("sounds/click.ogg") == null)
            }
        }
    }

    @Test
    fun acceptsRootManifestAndRepacks() {
        zipOf(
            "manifest.json" to manifest("root-1"),
            "textures/item_texture.json" to "{}".toByteArray(),
        ).use { source ->
            val discovered = ResourcePackImporter.locatePacks(source)
            assertEquals(1, discovered.size)
            assertEquals("", discovered[0].root)

            val target = File(tempFolder.root, "root-1.zip")
            ResourcePackImporter.repack(source, discovered[0], target)
            ZipFile(target).use { result ->
                assertEquals(setOf("manifest.json", "textures/item_texture.json"), result.entryNames())
            }
        }
    }

    @Test
    fun rejectsNonResourceRootManifestAndArchivesWithoutPacks() {
        zipOf("manifest.json" to manifest("bp", type = "data")).use { source ->
            assertThrows(IllegalArgumentException::class.java) {
                ResourcePackImporter.locatePacks(source)
            }
        }
        zipOf("readme.txt" to "no packs here".toByteArray()).use { source ->
            assertThrows(IllegalArgumentException::class.java) {
                ResourcePackImporter.locatePacks(source)
            }
        }
    }

    @Test
    fun fallsBackToSanitizedFilenameWhenUuidMissing() {
        assertEquals(
            "my_pack_name",
            ResourcePackImporter.packId(DiscoveredPack("rp", null), "my pack name"),
        )
        assertEquals(
            "res_1",
            ResourcePackImporter.packId(DiscoveredPack("rp", "res/1"), "fallback"),
        )
        assertEquals(
            "fallback",
            ResourcePackImporter.packId(DiscoveredPack("rp", "   "), "fallback"),
        )
    }

    @Test
    fun repackStoresAlreadyCompressedTexturesAndKeepsTheirContent() {
        val texture = ByteArray(32 * 1024) { (it * 31 and 0xff).toByte() }
        zipOf(
            "manifest.json" to manifest("stored"),
            "pack_icon.png" to texture,
            "textures/blocks/stone.png" to texture,
            "textures/terrain_texture.json" to "{}".toByteArray(),
        ).use { source ->
            val target = File(tempFolder.root, "stored.zip")
            ResourcePackImporter.repack(source, ResourcePackImporter.locatePacks(source)[0], target)

            ZipFile(target).use { result ->
                assertEquals(ZipEntry.STORED, result.getEntry("textures/blocks/stone.png").method)
                assertEquals(ZipEntry.STORED, result.getEntry("pack_icon.png").method)
                assertEquals(ZipEntry.DEFLATED, result.getEntry("manifest.json").method)
                assertArrayEquals(
                    texture,
                    result.getInputStream(result.getEntry("textures/blocks/stone.png")).use { it.readBytes() },
                )
            }
        }
    }

    @Test
    fun repackOverwritesExistingTarget() {
        val target = File(tempFolder.root, "res-1.zip")
        target.writeText("OLD")
        zipOf(
            "manifest.json" to manifest("res-1"),
            "textures/a.png" to "NEW".toByteArray(),
        ).use { source ->
            ResourcePackImporter.repack(source, ResourcePackImporter.locatePacks(source)[0], target)
        }
        ZipFile(target).use { result ->
            assertEquals(setOf("manifest.json", "textures/a.png"), result.entryNames())
        }
        assertEquals(false, File(tempFolder.root, "res-1.zip.tmp").exists())
    }
}
