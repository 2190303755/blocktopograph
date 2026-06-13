package com.mithrilmania.blocktopograph.chunk

import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTOutput
import com.mithrilmania.blocktopograph.nbt.io.readAsCompound
import com.mithrilmania.blocktopograph.nbt.io.writeNBT
import com.mithrilmania.blocktopograph.world.chunk.ChunkTag
import org.iq80.leveldb.DBException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class NBTChunkData(chunk: Chunk, val dataType: ChunkTag) : ChunkData(chunk) {
    @JvmField
    val tags: LinkedHashMap<String, BinaryTag> = linkedMapOf()

    @Throws(DBException::class, IOException::class)
    fun load() {
        val chunk: Chunk = this.chunk.get() ?: return
        loadFromByteArray(
            chunk.worldData.getChunkData(
                chunk.mChunkX,
                chunk.mChunkZ,
                dataType,
                chunk.mDimension
            )
        )
    }

    @Throws(IOException::class)
    fun loadFromByteArray(data: ByteArray?) {
        if (data === null || data.isEmpty()) return
        val input = BedrockNBTInput(ByteArrayInputStream(data))
        val tags = input.readAsCompound(linkedMapOf())
        input.close()
        this.tags.clear()
        this.tags.putAll(tags)
    }

    @Throws(DBException::class, IOException::class)
    override fun write() {
        val bytes = ByteArrayOutputStream()
        val output = BedrockNBTOutput(bytes)
        this.tags.forEach { (key, tag) ->
            output.writeNBT(key, tag)
        }
        output.close()
        val chunk: Chunk = this.chunk.get() ?: return
        chunk.worldData.writeChunkData(
            chunk.mChunkX,
            chunk.mChunkZ,
            chunk.mDimension,
            this.dataType,
            bytes.toByteArray()
        )
    }

    override fun createEmpty() {
        this.tags["Placeholder"] = IntTag(42)
    }
}
