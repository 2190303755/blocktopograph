package com.mithrilmania.blocktopograph.world

import android.content.Context
import android.util.Log
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.nbt.util.TagWithMeta
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.util.APP_TAG
import java.io.IOException

class WorldConfig(
    @JvmField val source: File,
    context: Context
) : NBTSource {
    @JvmField
    val appContext: Context = context.applicationContext
    private var cache: CompoundTag? = null

    suspend fun getCached(): CompoundTag {
        if (this.cache === null) {
            this.readNBT(this.appContext, NBTImportConfigImpl())
        }
        return this.cache ?: CompoundTag()
    }

    override suspend fun readNBT(context: Context, config: NBTImportConfig): TagWithMeta? {
        val result = try {
            this.source.readNBT(context, config)
        } catch (e: IOException) {
            Log.e(APP_TAG, "Failed to read $source", e)
            return null
        }
        if (result?.tag is CompoundTag) {
            this.cache = result.tag
        } else {
            Log.w(APP_TAG, "Failed to read $source as compound")
        }
        return result
    }

    override suspend fun saveNBT(
        context: Context,
        config: NBTExportConfig,
        name: String,
        tag: BinaryTag
    ) {
        this.source.saveNBT(context, config, name, tag)
        if (tag is CompoundTag) {
            this.cache = tag
        }
    }

    override fun resolveName(context: Context): String =
        this.source.resolveName(context)

    override fun describeSource(): String =
        this.source.describeSource()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.source == (other as WorldConfig).source
    }

    override fun hashCode(): Int = this.source.hashCode()
}