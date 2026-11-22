package com.mithrilmania.blocktopograph.texture

import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class TextureAtlasJsonTest {

    @Test
    fun parsesCommentUnknownKeysAndAllTextureShapes() {
        val atlas = lenientTextureJson.decodeFromString<TextureAtlasJson>(
            """
            // copied from a resource pack, with a leading comment
            {
              "resource_pack_name": "vanilla",
              "texture_name": "atlas.terrain",
              "padding": 8,
              "future_field": { "whatever": true },
              "texture_data": {
                "plain": { "textures": "textures/blocks/stone" },
                "array": { "textures": ["textures/blocks/stone", "textures/blocks/dirt"] },
                "object": {
                  "textures": [
                    { "path": "textures/blocks/grass_side", "overlay_color": "#df6827" },
                    { "path": "textures/blocks/grass_top", "tint_color": "#79c05a" }
                  ]
                },
                "empty": { "textures": [] },
                "noPath": { "textures": [ { "overlay_color": "#ffffff" } ] },
                "entryTint": {
                  "textures": "textures/blocks/dirt",
                  "overlay_color": "#ff0000",
                  "tint_color": "#00ff00"
                },
                "nullable": { "textures": "textures/blocks/dirt", "overlay_color": null }
              }
            }
            """.trimIndent()
        )

        assertEquals("vanilla", atlas.resourcePackName)
        assertEquals("atlas.terrain", atlas.textureName)
        assertEquals("textures/blocks/stone", atlas.textureData.getValue("plain").basePath())
        assertEquals("textures/blocks/stone", atlas.textureData.getValue("array").basePath())
        assertEquals("textures/blocks/grass_side", atlas.textureData.getValue("object").basePath())
        assertNull(atlas.textureData.getValue("empty").basePath())
        assertNull(atlas.textureData.getValue("noPath").basePath())
        assertEquals("textures/blocks/dirt", atlas.textureData.getValue("nullable").basePath())
        assertEquals("#ff0000", atlas.textureData.getValue("entryTint").overlayColor)
        assertEquals("#00ff00", atlas.textureData.getValue("entryTint").tintColor)
    }

    @Test
    fun toleratesMissingFieldsAndKeepsNamespace() {
        val atlas = lenientTextureJson.decodeFromString<TextureAtlasJson>(
            """{"resource_pack_name": "mypack", "texture_data": {}}"""
        )
        assertEquals("mypack", atlas.resourcePackName)
        assertEquals("", atlas.textureName)
        assertTrue(atlas.textureData.isEmpty())
    }

    @Test
    fun parsesRealTerrainAtlasFromTempTextures() {
        val file = atlasFile("terrain_texture.json")
        val atlas = lenientTextureJson.decodeFromString<TextureAtlasJson>(file.readText())

        assertEquals("vanilla", atlas.resourcePackName)
        assertEquals("atlas.terrain", atlas.textureName)
        assertTrue(atlas.textureData.size > 1000)
        assertEquals("textures/blocks/stone", atlas.textureData.getValue("stone").basePath())
        assertEquals("textures/blocks/brick", atlas.textureData.getValue("brick").basePath())
        assertEquals(
            "textures/blocks/grass_side",
            atlas.textureData.getValue("grass_side").basePath()
        )
        assertEquals(
            "textures/blocks/grass_top",
            atlas.textureData.getValue("grass_top").basePath()
        )
    }

    @Test
    fun parsesRealItemAtlasFromTempTextures() {
        val file = atlasFile("item_texture.json")
        val atlas = lenientTextureJson.decodeFromString<TextureAtlasJson>(file.readText())

        assertEquals("vanilla", atlas.resourcePackName)
        assertEquals("atlas.items", atlas.textureName)
        assertEquals("textures/items/apple", atlas.textureData.getValue("apple").basePath())
        assertEquals("textures/items/wood_axe", atlas.textureData.getValue("axe").basePath())
    }

    /** temp/textures is not checked in; skip when it is missing. */
    private fun atlasFile(name: String): File {
        val dir = System.getProperty("texture.fixtures.dir")?.let(::File)
        assumeTrue("temp/textures not found; skipping the real atlas parsing test", dir?.isDirectory == true)
        val file = File(dir, name)
        assumeTrue("$name not found; skipping the real atlas parsing test", file.isFile)
        return file
    }
}
