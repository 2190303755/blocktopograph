package com.mithrilmania.blocktopograph.block

import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.chunk.terrain.V1d2d13TerrainSubChunk
import com.mithrilmania.blocktopograph.chunk.terrain.V1d2d13TerrainSubChunk.BlockStorage
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTOutput
import com.mithrilmania.blocktopograph.nbt.io.readNamedTag
import com.mithrilmania.blocktopograph.nbt.io.writeNBT
import java.io.Serializable


private fun BlockType?.getKnownPropertyIndex(prop: String): Int = if (this === null) {
    -1
} else {
    this.knownProperties.indexOfFirst { prop == it.name }
}

fun BedrockNBTInput.readBlockFormV1d2d13TerrainSubChunk(): Block {
    val block = this.readNamedTag().second
    if (block !is CompoundTag) {
        throw ClassCastException()
    }
    val name = requireNotNull(block[BlockStorage.PALETTE_KEY_NAME] as? StringTag).value
    val type = BlockType.get(name)
    val builder = if (type === null) Block.Builder(name) else Block.Builder(type)
    (block[BlockStorage.PALETTE_KEY_STATES] as? CompoundTag)?.let {
        it.forEach { (key, tag) ->
            builder.setProperty(key, tag)
        }
    }
    block[BlockStorage.PALETTE_KEY_VERSION]?.let {
        when (it) {
            is NumericTag -> it.toNumber().toString()
            is StringTag -> it.toString()
            else -> null
        }?.let { value ->
            if (!V1d2d13TerrainSubChunk.VERSIONS.contains(value)) {
                LogUtil.d(BlockStorage::class.java, "fuckfuckversion:$value") // sic
                V1d2d13TerrainSubChunk.VERSIONS.add(value)
            }
        }
    }
    return builder.build()
}

fun BedrockNBTOutput.writeBlockIntoV1d2d13TerrainSubChunk(block: Block) {
    val root = hashMapOf<String, BinaryTag>()
    val states = hashMapOf<String, BinaryTag>()
    val props = block.type?.knownProperties
    val values = block.knownProperties
    if (props !== null && values !== null) {
        val size = minOf(props.size, values.size)
        for (i in 0 until size) {
            states[props[i].name] = values[i] ?: continue
        }
    }
    states.putAll(block.customProperties)
    root[BlockStorage.PALETTE_KEY_NAME] = StringTag(block.name)
    root[BlockStorage.PALETTE_KEY_STATES] = CompoundTag(states)
    root[BlockStorage.PALETTE_KEY_VERSION] = IntTag(2012)
    this.writeNBT(BlockStorage.PALETTE_KEY_ROOT, CompoundTag(root))
}

class Block internal constructor(
    @JvmField
    val name: String,
    @JvmField
    val type: BlockType?,
    @JvmField
    val knownProperties: Array<BinaryTag?>?,
    @JvmField
    val customProperties: MutableMap<String, BinaryTag>
) : Serializable {
    fun getProperty(name: String): Any? {
        val index: Int = this.type.getKnownPropertyIndex(name)
        val tag = if (index < 0 || this.knownProperties === null) {
            this.customProperties[name]
        } else {
            this.knownProperties[index]
        }
        return when (tag) {
            is NumericTag -> tag.toNumber()
            is StringTag -> tag.toString()
            else -> null
        }
    }

    class Builder {
        private val name: String?
        private val type: BlockType?
        private val knownProperties: Array<BinaryTag?>?
        private val customProperties: MutableMap<String, BinaryTag> = hashMapOf()

        constructor(name: String) {
            this.name = name
            this.type = null
            this.knownProperties = null
        }

        constructor(type: BlockType) {
            this.name = null
            this.type = type
            this.knownProperties = arrayOfNulls(type.knownProperties.size)
        }

        fun setProperty(name: String, tag: BinaryTag): Builder {
            val index: Int = this.type.getKnownPropertyIndex(name)
            if (index < 0 || this.knownProperties === null) {
                this.customProperties[name] = tag
            } else {
                this.knownProperties[index] = tag
            }
            return this
        }

        fun setProperty(name: String, value: String): Builder =
            this.setProperty(name, StringTag(value))

        fun setProperty(name: String, value: Byte): Builder =
            this.setProperty(name, ByteTag(value))

        fun setProperty(name: String, value: Int): Builder =
            this.setProperty(name, IntTag(value))

        fun build(): Block = if (this.type === null) {
            Block(requireNotNull(this.name), null, null, this.customProperties)
        } else {
            Block(this.type.name, this.type, this.knownProperties, this.customProperties)
        }
    }
}
