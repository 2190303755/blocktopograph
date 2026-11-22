package com.mithrilmania.blocktopograph.texture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextureRequestFactoryTest {

    private fun lookup(
        blocks: String = "{}",
        items: String = "{}",
    ): TextureLookup = buildTextureLookupFromAtlases(
        listOf(
            PackAtlases(
                blocks = lenientTextureJson.decodeFromString(blocks),
                items = lenientTextureJson.decodeFromString(items),
            ),
        ),
    )

    private val blockAtlas = """
        {"resource_pack_name":"vanilla","texture_data":{
          "brick":{"textures":"textures/blocks/brick"},
          "stone":{"textures":"textures/blocks/stone"}
        }}
    """.trimIndent()

    private val itemAtlas = """
        {"resource_pack_name":"vanilla","texture_data":{
          "apple":{"textures":"textures/items/apple"},
          "brick":{"textures":"textures/items/brick"}
        }}
    """.trimIndent()

    @Test
    fun manualBindingWinsAndHeuristicFallsBack() {
        val factory = TextureRequestFactory(
            lookup = lookup(blocks = blockAtlas),
            blockBindings = mapOf("minecraft:brick_block" to "brick"),
        )

        assertEquals(
            TextureRequest("textures/blocks/brick", factory.lookup.version),
            factory.createBlockRequest("minecraft:brick_block"),
        )
        // no manual binding: the bare identifier is used as-is
        assertEquals(
            TextureRequest("textures/blocks/stone", factory.lookup.version),
            factory.createBlockRequest("minecraft:stone"),
        )
    }

    @Test
    fun blockAndItemBindingsAreIndependent() {
        val factory = TextureRequestFactory(
            lookup = lookup(blocks = blockAtlas, items = itemAtlas),
            blockBindings = mapOf("shared" to "brick", "only_block" to "stone"),
            itemBindings = mapOf("shared" to "apple", "only_item" to "brick"),
        )

        assertEquals("textures/blocks/brick", factory.createBlockRequest("shared")?.path)
        assertEquals("textures/items/apple", factory.createItemRequest("shared")?.path)
        assertNull(factory.createItemRequest("only_block"))
        assertNull(factory.createBlockRequest("only_item"))
    }

    @Test
    fun manualBindingCanTargetDedicatedNamespace() {
        val factory = TextureRequestFactory(
            lookup = lookup(
                blocks = """
                    {"resource_pack_name":"farm","texture_data":{
                      "cabbage":{"textures":"textures/blocks/cabbage"}
                    }}
                """.trimIndent(),
            ),
            blockBindings = mapOf("farm:cabbage_block" to "farm:cabbage"),
        )

        assertEquals(
            "textures/blocks/cabbage",
            factory.createBlockRequest("farm:cabbage_block")?.path,
        )
        // the guess drops the namespace, so the bare name in the dedicated table is out of reach
        assertNull(factory.createBlockRequest("farm:cabbage"))
    }

    @Test
    fun requestsAreNullOnMissAndFactoryChangesWhenLookupOrBindingsChange() {
        val empty = TextureRequestFactory.EMPTY
        assertNull(empty.createBlockRequest("minecraft:stone"))
        assertNull(empty.createItemRequest("apple"))

        val first = TextureRequestFactory(lookup(blocks = blockAtlas))
        val sameLookup = first.copy(blockBindings = mapOf("minecraft:brick_block" to "brick"))
        assertTrue(first != sameLookup)
        assertEquals("textures/blocks/brick", sameLookup.createBlockRequest("minecraft:brick_block")?.path)

        val rebuilt = TextureRequestFactory(lookup(blocks = blockAtlas))
        assertTrue(rebuilt.lookup.version > first.lookup.version)
        assertTrue(first != rebuilt)
        assertTrue(rebuilt.createBlockRequest("minecraft:stone")!!.version > first.createBlockRequest("minecraft:stone")!!.version)
    }
}
