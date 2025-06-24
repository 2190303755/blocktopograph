package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.Log
import com.mithrilmania.blocktopograph.nbt.old.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.nbt.old.tags.CompoundTag
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.WorldHandler
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.iq80.leveldb.Options
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class ShizukuWorldHandler(
    root: String,
    name: String
) : WorldHandler(name, root) {
    override fun load(context: Context) {
        val service = Blocktopograph.fileService ?: return
        try {
            service.getFileDescriptor(this.path + '/' + FILE_LEVEL_DAT)?.use {
                this.dataCompat = LevelDataConverter.read(FileInputStream(it.fileDescriptor))
            }
        } catch (e: IOException) {
            Log.e(this, e)
        }
    }

    override fun save(context: Context, data: CompoundTag) {
        val service = Blocktopograph.fileService ?: return
        try {
            service.getFileDescriptor(this.path + '/' + FILE_LEVEL_DAT)?.use {
                LevelDataConverter.write(FileOutputStream(it.fileDescriptor), data)
            }
        } catch (e: IOException) {
            Log.e(this, e)
        }
        this.dataCompat = data
    }

    override fun open(scope: CoroutineScope, context: Context) = scope.async(Dispatchers.IO) {
        if (this@ShizukuWorldHandler.storage != null) return@async this@ShizukuWorldHandler.storage
        try {
            val service = Blocktopograph.fileService ?: return@async null
            this@ShizukuWorldHandler.storage = WorldStorage(
                service.prepareDB(
                    context.externalCacheDir?.absolutePath ?: return@async null,
                    this@ShizukuWorldHandler.path
                ) ?: return@async null,
                Options.newDefaultOptions()
            )
            return@async this@ShizukuWorldHandler.storage
        } catch (e: IOException) {
            Log.e(this@ShizukuWorldHandler, e)
            return@async null
        }
    }

    override fun sync(scope: CoroutineScope, context: Context) {
        TODO("Not yet implemented")
    }
}