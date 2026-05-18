package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import android.net.Uri
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.util.copyFolderTo
import com.mithrilmania.blocktopograph.util.error
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldStorage
import org.iq80.leveldb.Options
import java.io.File
import java.io.IOException
import java.util.UUID

class SAFWorld(
    val root: Uri,
    config: Uri,
    name: String?
) : World(
    name,
    SAFFile(config)
) {
    override suspend fun open(context: Context): WorldStorage? {
        if (this.storage != null) return this.storage
        val cache = context.externalCacheDir?.path ?: return null
        var folder: File? = null
        try {
            val resolver = context.contentResolver
            val source = this.root.findChild(resolver, "db") ?: return null
            do {
                folder = File(cache, UUID.randomUUID().toString())
            } while (folder.exists())
            source.copyFolderTo(resolver, folder)
            this.storage = WorldStorage(folder.path, Options.newDefaultOptions())
        } catch (e: IOException) {
            e.error("Failed to open level db at ${folder?.path} from $root")
        }
        return this.storage
    }

    override suspend fun sync(context: Context) {
        TODO("Not yet implemented")
    }
}