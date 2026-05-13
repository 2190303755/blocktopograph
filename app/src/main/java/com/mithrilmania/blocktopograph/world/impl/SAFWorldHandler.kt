package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import android.net.Uri
import com.mithrilmania.blocktopograph.nbt.old.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.util.copyFolderTo
import com.mithrilmania.blocktopograph.util.error
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.world.WorldHandler
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.CoroutineScope
import org.iq80.leveldb.Options
import java.io.File
import java.io.IOException
import java.util.UUID

class SAFWorldHandler(
    val root: Uri,
    override val config: SAFFile,
    name: String
) : WorldHandler(
    name,
    root.lastPathSegment ?: root.toString()
) {
    constructor(root: Uri, config: Uri, name: String) : this(root, SAFFile(config), name)

    override fun loadCompat(context: Context) {
        try {
            this.dataCompat = this.config.read(context, LevelDataConverter::read)
        } catch (e: IOException) {
            e.error("Failed to read level.dat with url: $config")
        }
    }

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

    override fun sync(scope: CoroutineScope, context: Context) {
        TODO("Not yet implemented")
    }
}