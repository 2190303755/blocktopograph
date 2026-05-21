package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.util.error
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldStorage
import org.iq80.leveldb.Options
import java.io.IOException

class ShizukuWorld(
    val root: String,
    name: String?
) : World(name, ShizukuFile("$root/$FILE_LEVEL_DAT")) {
    override suspend fun open(context: Context): WorldStorage? {
        try {
            val service = Blocktopograph.fileService ?: return null
            return WorldStorage(
                service.prepareDB(
                    context.externalCacheDir?.absolutePath ?: return null,
                    this.root
                ) ?: return null,
                Options.newDefaultOptions()
            )
        } catch (e: IOException) {
            e.error("Failed to open level db from $root")
        }
        return null
    }

    override suspend fun sync(context: Context) {
        TODO("Not yet implemented")
    }
}