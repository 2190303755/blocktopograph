package com.mithrilmania.blocktopograph.nbt.io

import android.content.Context
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.storage.File

interface NBTSource {
    fun readNBT(context: Context, config: NBTImportConfig): TagWithMeta?
    fun saveNBT(context: Context, config: NBTExportConfig, name: String, tag: BinaryTag)
    fun resolveName(context: Context): String
}

class LocalPlayerSource(
    val dat: File
) : NBTSource, NBTImportConfig {
    override var format: NBTFormat = NBTFormat.LITTLE_ENDIAN
    override var header: HeaderPresence = HeaderPresence.PRESENT
    override fun readNBT(
        context: Context,
        config: NBTImportConfig
    ): TagWithMeta? {
        val current = this.dat.readNBT(context, config)
        if (current === null) {
            this.format = NBTFormat.LITTLE_ENDIAN
            this.header = HeaderPresence.PRESENT
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
            this.header = if (current.version === null) {
                HeaderPresence.UNCERTAIN
            } else {
                HeaderPresence.PRESENT
            }
        }
        return null
    }

    override fun saveNBT(
        context: Context,
        config: NBTExportConfig,
        name: String,
        tag: BinaryTag
    ) {
        val current = this.dat.readNBT(context, this)
        if (current?.tag is CompoundTag) {
            current.tag[KEY_LOCAL_PLAYER] = tag
            this.dat.saveNBT(context, config, current.name, current.tag)
        }
    }

    override fun resolveName(context: Context): String =
        this.dat.resolveName(context) + " > $KEY_LOCAL_PLAYER"

    companion object {
        const val KEY_LOCAL_PLAYER = "Player"
    }
}