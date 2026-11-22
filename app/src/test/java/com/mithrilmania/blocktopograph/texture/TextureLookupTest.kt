package com.mithrilmania.blocktopograph.texture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class TextureLookupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun atlas(json: String): TextureAtlasJson = lenientTextureJson.decodeFromString(json)

    private fun pack(blocks: String = "{}", items: String = "{}") = PackAtlases(
        blocks = atlas(blocks),
        items = atlas(items),
    )

    private fun zipOf(vararg entries: Pair<String, String>): ZipFile {
        val file: File = tempFolder.newFile()
        ZipOutputStream(file.outputStream()).use { out ->
            for ((name, content) in entries) {
                out.putNextEntry(ZipEntry(name))
                out.write(content.toByteArray(Charsets.UTF_8))
                out.closeEntry()
            }
        }
        return ZipFile(file)
    }

    @Test
    fun resolvesSharedEntriesAndPrefersDedicatedNamespace() {
        val shared = """
            {"resource_pack_name": "vanilla", "texture_data": {
              "stone": {"textures": "textures/blocks/stone"},
              "farm:cabbage": {"textures": "textures/blocks/cabbage_shared"}
            }}
        """.trimIndent()
        val dedicated = """
            {"resource_pack_name": "farm", "texture_data": {
              "cabbage": {"textures": "textures/blocks/cabbage_farm"}
            }}
        """.trimIndent()
        val lookup = buildTextureLookupFromAtlases(listOf(pack(shared), pack(dedicated)))

        assertEquals("textures/blocks/stone", lookup.blocks.find("", "stone"))
        assertEquals("textures/blocks/stone", lookup.blocks.find("minecraft", "stone"))
        // once a dedicated table exists it answers alone; "farm:cabbage" in shared is out of play
        assertEquals("textures/blocks/cabbage_farm", lookup.blocks.find("farm", "cabbage"))
        assertNull(lookup.blocks.find("farm", "stone"))
    }

    @Test
    fun namespaceKeyInSharedIsFoundWhenNoDedicatedTable() {
        val shared = """
            {"resource_pack_name": "", "texture_data": {
              "farm:cabbage": {"textures": "textures/blocks/cabbage"}
            }}
        """.trimIndent()
        val lookup = buildTextureLookupFromAtlases(listOf(pack(shared)))

        assertEquals("textures/blocks/cabbage", lookup.blocks.find("farm", "cabbage"))
        assertNull(lookup.blocks.find("farm", "unknown"))
    }

    @Test
    fun higherPriorityPackWinsForSharedAndDedicated() {
        val high = pack(
            blocks = """
                {"resource_pack_name": "vanilla", "texture_data": {
                  "stone": {"textures": "textures/blocks/stone_high"}
                }}
            """.trimIndent(),
        )
        val highDedicated = pack(
            blocks = """
                {"resource_pack_name": "farm", "texture_data": {
                  "cabbage": {"textures": "textures/blocks/cabbage_high"}
                }}
            """.trimIndent(),
        )
        val low = pack(
            blocks = """
                {"resource_pack_name": "vanilla", "texture_data": {
                  "stone": {"textures": "textures/blocks/stone_low"},
                  "dirt": {"textures": "textures/blocks/dirt_low"}
                }}
            """.trimIndent(),
            items = """
                {"texture_name": "atlas.items", "texture_data": {
                  "apple": {"textures": "textures/items/apple"}
                }}
            """.trimIndent(),
        )
        val lowDedicated = pack(
            blocks = """
                {"resource_pack_name": "farm", "texture_data": {
                  "cabbage": {"textures": "textures/blocks/cabbage_low"}
                }}
            """.trimIndent(),
        )
        val lookup = buildTextureLookupFromAtlases(listOf(high, highDedicated, low, lowDedicated))

        assertEquals("textures/blocks/stone_high", lookup.blocks.find("", "stone"))
        assertEquals("textures/blocks/dirt_low", lookup.blocks.find("", "dirt"))
        assertEquals("textures/blocks/cabbage_high", lookup.blocks.find("farm", "cabbage"))
        assertEquals("textures/items/apple", lookup.items.find("", "apple"))
    }

    @Test
    fun everyRebuildGetsAFreshMonotonicVersion() {
        val first = buildTextureLookupFromAtlases(listOf(pack()))
        val second = buildTextureLookupFromAtlases(listOf(pack()))

        assertTrue(second.version > first.version)
        assertTrue(TextureLookup.EMPTY.version > 0)
    }

    @Test
    fun readsZipsAndToleratesMissingAtlasesAndEntries() {
        val itemsOnly = zipOf(
            ITEM_ATLAS_PATH to """
                {"texture_name": "atlas.items", "texture_data": {
                  "apple": {"textures": "textures/items/apple"}
                }}
            """.trimIndent(),
        ).use { zip -> buildTextureLookupFromZips(listOf(zip)) }

        assertTrue(itemsOnly.blocks.shared.isEmpty())
        assertEquals("textures/items/apple", itemsOnly.items.find("", "apple"))

        val terrainPack = zipOf(
            TERRAIN_ATLAS_PATH to """
                // real atlas json starts with a comment
                {"resource_pack_name": "vanilla", "texture_data": {
                  "stone": {"textures": "textures/blocks/stone"},
                  "grass_side": {"textures": [{"path": "textures/blocks/grass_side"}]},
                  "empty": {"textures": []},
                  "bad": {"textures": {"path": "textures/blocks/bad"}}
                }}
            """.trimIndent(),
        )
        val lookup = terrainPack.use { zip -> buildTextureLookupFromZips(listOf(zip)) }

        assertTrue(lookup.items.shared.isEmpty())
        assertEquals("textures/blocks/stone", lookup.blocks.find("", "stone"))
        assertEquals("textures/blocks/grass_side", lookup.blocks.find("", "grass_side"))
        assertNull(lookup.blocks.find("", "empty"))
        assertNull(lookup.blocks.find("", "bad"))

        zipOf().use { empty ->
            val emptyLookup = buildTextureLookupFromZips(listOf(empty))
            assertTrue(emptyLookup.blocks.shared.isEmpty())
            assertTrue(emptyLookup.items.shared.isEmpty())
        }
    }
}
