package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import android.net.Uri
import com.mithrilmania.blocktopograph.Log
import com.mithrilmania.blocktopograph.nbt.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag
import com.mithrilmania.blocktopograph.util.copyFolderTo
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.world.WorldHandler
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.iq80.leveldb.Options
import java.io.File
import java.io.IOException
import java.util.UUID

class SAFWorldHandler(
    val root: Uri,
    private val config: Uri,
    name: String
) : WorldHandler(
    name,
    root.lastPathSegment ?: root.toString()
) {
    override fun load(context: Context) {
        try {
            this.data = LevelDataConverter.read(
                context.contentResolver.openInputStream(this.config)
            )
        } catch (e: IOException) {
            Log.e(this, e)
        }
    }

    override fun save(context: Context, data: CompoundTag) {
        try {
            LevelDataConverter.write(context.contentResolver.openOutputStream(this.config), data)
        } catch (e: IOException) {
            Log.e(this, e)
        }
        this.data = data
    }

    override fun open(scope: CoroutineScope, context: Context) = scope.async(Dispatchers.IO) {
        if (this@SAFWorldHandler.storage != null) return@async this@SAFWorldHandler.storage
        val cache = context.externalCacheDir?.path ?: return@async null
        try {
            val resolver = context.contentResolver
            val location = this@SAFWorldHandler.root
            val source = location.findChild(resolver, "db") ?: return@async null
            var folder: File
            do {
                folder = File(cache, UUID.randomUUID().toString())
            } while (folder.exists())
            source.copyFolderTo(resolver, folder)
            this@SAFWorldHandler.storage = WorldStorage(folder.path, Options.newDefaultOptions())
            return@async this@SAFWorldHandler.storage
        } catch (e: IOException) {
            Log.e(this@SAFWorldHandler, e)
            return@async null
        }
    }

    override fun sync(scope: CoroutineScope, context: Context) {
        TODO("Not yet implemented")
    }
}