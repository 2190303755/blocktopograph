package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.nbt.old.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.util.error
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.WorldHandler
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.CoroutineScope
import org.iq80.leveldb.Options
import java.io.FileInputStream
import java.io.IOException

class ShizukuWorldHandler(
    root: String,
    name: String
) : WorldHandler(name, root) {
    override val config: ShizukuFile = ShizukuFile(this.path + '/' + FILE_LEVEL_DAT)
    override fun loadCompat(context: Context) {
        val service = Blocktopograph.fileService ?: return
        val config = this.path + '/' + FILE_LEVEL_DAT
        try {
            service.getFileDescriptor(config)?.use {
                this.dataCompat = LevelDataConverter.read(FileInputStream(it.fileDescriptor))
            }
        } catch (e: IOException) {
            e.error("Failed to read level.dat with path: $config")
        }
    }

    override suspend fun open(context: Context): WorldStorage? {
        if (this.storage != null) return this.storage
        try {
            val service = Blocktopograph.fileService ?: return null
            this.storage = WorldStorage(
                service.prepareDB(
                    context.externalCacheDir?.absolutePath ?: return null,
                    this.path
                ) ?: return null,
                Options.newDefaultOptions()
            )
        } catch (e: IOException) {
            e.error("Failed to open level db from $path")
        }
        return this.storage
    }

    override fun sync(scope: CoroutineScope, context: Context) {
        TODO("Not yet implemented")
    }
}