package com.mithrilmania.blocktopograph.world

import android.content.Context
import android.util.Log
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.nbt.io.TagWithMeta
import com.mithrilmania.blocktopograph.nbt.io.readNBT
import com.mithrilmania.blocktopograph.nbt.io.writeNBT
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.error
import java.io.IOException

class WorldConfig(
    val source: File
) : NBTSource {
    private var cache: CompoundTag? = null

    fun getCached(context: Context? = null): CompoundTag {
        if (this.cache === null && context !== null) {
            this.readNBT(context, NBTImportConfigImpl())
        }
        return this.cache ?: CompoundTag()
    }

    override fun readNBT(context: Context, config: NBTImportConfig): TagWithMeta? {
        val result = try {
            this.source.read(context) { it.readNBT(config) }
        } catch (e: IOException) {
            e.error("Failed to read $source")
            return null
        }
        if (result?.tag is CompoundTag) {
            this.cache = result.tag
        } else {
            Log.w(APP_TAG, "Failed to read $source as compound")
        }
        return result
    }

    override fun saveNBT(context: Context, config: NBTExportConfig, name: String, tag: BinaryTag) {
        this.source.save(context) {
            it.writeNBT(name, tag, config)
            if (tag is CompoundTag) {
                this.cache = tag
            }
        }
    }

    override fun resolveName(context: Context): String {
        return this.source.resolveName(context)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.source == (other as WorldConfig).source
    }

    override fun hashCode(): Int = this.source.hashCode()
}