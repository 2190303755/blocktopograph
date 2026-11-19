package com.mithrilmania.blocktopograph.world.access

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.core.content.FileProvider
import com.mithrilmania.blocktopograph.FILE_PROVIDER_AUTH
import com.mithrilmania.blocktopograph.map.MarkerManager
import com.mithrilmania.blocktopograph.settings.WorkspaceStore
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.storage.JvmFile
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.storage.Workspace
import com.mithrilmania.blocktopograph.storage.awaitFileService
import com.mithrilmania.blocktopograph.storage.resolve
import com.mithrilmania.blocktopograph.storage.uuid
import com.mithrilmania.blocktopograph.util.LEVEL_DB_TAG
import com.mithrilmania.blocktopograph.util.rpc
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.FILE_MARKERS
import com.mithrilmania.blocktopograph.world.FOLDER_DATABASE
import org.iq80.leveldb.impl.Filename
import java.io.IOException

class ShizukuWorldAccess(
    @JvmField val root: String,
    context: Context,
    name: String?,
    config: File = ShizukuFile("$root/$FILE_LEVEL_DAT")
) : WorldAccess(name, config, context) {
    override val identifier: String
        get() = this.root

    override val markers: MarkerManager by lazy {
        MarkerManager(ShizukuFile("$root/$FILE_MARKERS"), this.appContext)
    }

    override suspend fun queryPreparedWorkspaceAndValidate(): Workspace? {
        val cache = this.appContext.externalCacheDir ?: return null
        val previous = WorkspaceStore.read(this.appContext, this.root) ?: return null
        val folder = JvmFile(cache, previous.uuid.toString())
        if (!folder.isDirectory) return null
        val mtime = awaitFileService()?.rpc { service ->
            val currentPath = "$root/$FOLDER_DATABASE/${Filename.currentFileName()}"
            val fd = service.openReadOnly(currentPath) ?: return@rpc null
            val manifestName = ParcelFileDescriptor.AutoCloseInputStream(fd)
                .bufferedReader().use { it.readText().trim() }
            service.metadata("$root/$FOLDER_DATABASE/$manifestName")?.lastModifiedAtMillis
        } ?: return null
        if (previous.manifestModifiedMillis == mtime.fastCoerceAtLeast(0L)) {
            return previous.resolve(cache)
        }
        return null
    }

    override suspend fun prepareWorkspace(): Workspace? {
        val cache = this.appContext.externalCacheDir ?: return null
        try {
            WorkspaceStore.remove(this.appContext, this.root)?.let {
                JvmFile(cache, it.uuid.toString()).deleteRecursively()
            }
            val prepared = awaitFileService()?.rpc {
                it.prepareDB(cache.absolutePath, this.root, null)
            } ?: return null
            WorkspaceStore.write(this.appContext, this.root, prepared)
            return prepared.resolve(cache)
        } catch (e: IOException) {
            Log.e(LEVEL_DB_TAG, "Failed to open level db from $root", e)
        }
        return null
    }

    override suspend fun merge(leveldb: String): Uri? {
        val exported = awaitFileService()?.rpc {
            it.mergeDB(this.root, leveldb)
        } ?: return null
        return FileProvider.getUriForFile(
            this.appContext,
            FILE_PROVIDER_AUTH,
            JvmFile(exported)
        )
    }
}