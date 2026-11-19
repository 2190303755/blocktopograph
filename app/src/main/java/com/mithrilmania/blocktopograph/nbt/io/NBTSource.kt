package com.mithrilmania.blocktopograph.nbt.io

import android.content.Context
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.util.TagWithMeta

interface NBTSource {
    suspend fun readNBT(context: Context, config: NBTImportConfig): TagWithMeta?
    suspend fun saveNBT(context: Context, config: NBTExportConfig, name: String, tag: BinaryTag)
    fun resolveName(context: Context): String
    fun describeSource(): String
    override fun equals(other: Any?): Boolean
}

class LocalPlayerSource(
    @JvmField val dat: NBTSource
) : NBTSource, NBTImportConfig {
    override var format: NBTFormat = NBTFormat.LITTLE_ENDIAN
    override var header: Boolean? = true
    override suspend fun readNBT(
        context: Context,
        config: NBTImportConfig
    ): TagWithMeta? {
        val current = this.dat.readNBT(context, config)
        if (current === null) {
            this.format = NBTFormat.LITTLE_ENDIAN
            this.header = true
        } else {
            if (current.tag is CompoundTag) {
                val player = current.tag[KEY_LOCAL_PLAYER]
                if (player !== null) return current.copy(name = KEY_LOCAL_PLAYER, tag = player)
            }
            this.format = if (current.littleEndian) {
                NBTFormat.LITTLE_ENDIAN
            } else {
                NBTFormat.UNKNOWN
            }
            this.header = if (current.storageVersion === null) null else true
        }
        return null
    }

    override suspend fun saveNBT(
        context: Context,
        config: NBTExportConfig,
        name: String,
        tag: BinaryTag
    ) {
        val data = this.dat.readNBT(context, this)
        if (data?.tag is CompoundTag) {
            data.tag[KEY_LOCAL_PLAYER] = tag
            this.dat.saveNBT(context, config, data.name, data.tag)
        }
    }

    override fun resolveName(context: Context): String =
        this.dat.resolveName(context) + " > $KEY_LOCAL_PLAYER"

    override fun describeSource(): String =
        this.dat.describeSource() + " > $KEY_LOCAL_PLAYER"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.dat == (other as LocalPlayerSource).dat
    }

    override fun hashCode(): Int = this.dat.hashCode()

    companion object {
        const val KEY_LOCAL_PLAYER = "Player"
    }
}