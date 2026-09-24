package com.mithrilmania.blocktopograph.chunk

import android.util.Log
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTOutput
import com.mithrilmania.blocktopograph.nbt.io.readAsCompound
import com.mithrilmania.blocktopograph.nbt.io.writeNBT
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.chunk.ChunkTag
import org.iq80.leveldb.DBException
import java.io.ByteArrayOutputStream
import java.io.IOException

class NBTChunkData(chunk: Chunk, val dataType: ChunkTag) : ChunkData(chunk) {
    @JvmField
    val tags: LinkedHashMap<String, BinaryTag> = linkedMapOf()

    @Throws(DBException::class, IOException::class)
    fun load() {
        val chunk: Chunk = this.chunk.get() ?: return
        if (dataType == ChunkTag.ENTITY) {
            // TODO remove log
            Log.d(
                APP_TAG, "Loading Chunk Data ${
                    WorldStorage.makeChunkKey(
                        chunk.mChunkX,
                        chunk.mChunkZ,
                        chunk.mDimension.runtimeId,
                        dataType,
                    ).toHexString()
                }"
            )
        }
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
        val tags = BedrockNBTInput(data).use {
            it.readAsCompound(linkedMapOf())
        }
        this.tags.clear()
        this.tags.putAll(tags)
    }

    @Throws(DBException::class, IOException::class)
    override fun write() {
        val bytes = ByteArrayOutputStream()
        BedrockNBTOutput(bytes).use {
            this.tags.forEach { entry ->
                it.writeNBT(entry.key, entry.value)
            }
        }
        val chunk = this.chunk.get() ?: return
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
