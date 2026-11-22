package com.mithrilmania.blocktopograph.texture

import androidx.datastore.core.CorruptionException
import com.mithrilmania.blocktopograph.util.jsonSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TextureBindingStoreTest {

    @Test
    fun defaultsOnlyPopulateBlockAliases() {
        assertEquals(
            mapOf(
                "minecraft:brick_block" to "brick",
                "minecraft:lit_redstone_ore" to "redstone_ore",
            ),
            BUILTIN_BLOCK_TEXTURE_BINDINGS,
        )
        assertEquals(emptyMap<String, String>(), BUILTIN_ITEM_TEXTURE_BINDINGS)
    }

    @Test
    fun serializersRoundTripTheirOwnTableAndDefault() = runBlocking {
        val block = jsonSerializer { BUILTIN_BLOCK_TEXTURE_BINDINGS }
        val item = jsonSerializer { BUILTIN_ITEM_TEXTURE_BINDINGS }

        assertEquals(BUILTIN_BLOCK_TEXTURE_BINDINGS, block.defaultValue)
        assertEquals(BUILTIN_ITEM_TEXTURE_BINDINGS, item.defaultValue)

        val table = mapOf("minecraft:stone" to "stone", "minecraft:brick_block" to "brick")
        val output = ByteArrayOutputStream()
        block.writeTo(table, output)
        assertEquals(table, block.readFrom(ByteArrayInputStream(output.toByteArray())))

        val emptyOutput = ByteArrayOutputStream()
        item.writeTo(emptyMap(), emptyOutput)
        assertEquals(
            emptyMap<String, String>(),
            item.readFrom(ByteArrayInputStream(emptyOutput.toByteArray()))
        )
    }

    @Test
    fun corruptBindingFileIsReportedAsCorruption() = runBlocking {
        val serializer = jsonSerializer { BUILTIN_BLOCK_TEXTURE_BINDINGS }
        val result = runCatching {
            serializer.readFrom(ByteArrayInputStream("{ not json".toByteArray()))
        }
        assertTrue(result.exceptionOrNull() is CorruptionException)
    }
}
