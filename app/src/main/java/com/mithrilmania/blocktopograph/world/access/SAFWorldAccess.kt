package com.mithrilmania.blocktopograph.world.access

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.util.Log
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.PreparedWorkspace
import com.mithrilmania.blocktopograph.map.MarkerManager
import com.mithrilmania.blocktopograph.settings.WorkspaceStore
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.storage.JvmFile
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.storage.Workspace
import com.mithrilmania.blocktopograph.storage.resolve
import com.mithrilmania.blocktopograph.storage.uuid
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.LEVEL_DB_TAG
import com.mithrilmania.blocktopograph.util.copyContentsToDocument
import com.mithrilmania.blocktopograph.util.copyFolderTo
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.queryLong
import com.mithrilmania.blocktopograph.world.FILE_MARKERS
import com.mithrilmania.blocktopograph.world.FOLDER_DATABASE
import org.iq80.leveldb.impl.Filename
import java.io.IOException
import java.util.UUID

class SAFWorldAccess(
    @JvmField val root: Uri,
    config: File,
    context: Context,
    name: String?
) : WorldAccess(name, config, context) {
    override val identifier: String
        get() = this.root.toString()

    override val markers: MarkerManager by lazy {
        val resolver = this.appContext.contentResolver
        val markers = this.root.findChild(resolver, FILE_MARKERS)
            ?: DocumentsContract.createDocument(
                resolver, this.root, MIME_TYPE_DEFAULT, FILE_MARKERS
            )
            ?: error("Failed to create $FILE_MARKERS")
        MarkerManager(SAFFile(markers), this.appContext)
    }

    override suspend fun queryPreparedWorkspaceAndValidate(): Workspace? {
        val cacheDir = this.appContext.externalCacheDir ?: return null
        return try {
            val resolver = this.appContext.contentResolver
            val db = this.root.findChild(resolver, FOLDER_DATABASE) ?: return null
            val current = db.findChild(resolver, Filename.currentFileName()) ?: return null
            val manifestName = resolver.openInputStream(current)?.bufferedReader()?.use {
                it.readText().trim()
            } ?: return null

            val manifest = db.findChild(resolver, manifestName) ?: return null
            val mtime = manifest.queryLong(resolver, COLUMN_LAST_MODIFIED) ?: return null

            val previous =
                WorkspaceStore.read(this.appContext, this.identifier) ?: return null
            if (previous.manifestModifiedMillis != mtime) return null
            val folder = JvmFile(cacheDir, previous.uuid.toString())
            if (!folder.isDirectory) return null
            previous.resolve(cacheDir)
        } catch (e: IOException) {
            Log.e(LEVEL_DB_TAG, "Failed to validate prepared level db for $root", e)
            null
        }
    }

    override suspend fun prepareWorkspace(): Workspace? {
        val cacheDir = this.appContext.externalCacheDir ?: return null
        var folder: JvmFile? = null
        try {
            val resolver = this.appContext.contentResolver
            val db = this.root.findChild(resolver, FOLDER_DATABASE) ?: return null
            val current = db.findChild(resolver, Filename.currentFileName()) ?: return null
            val manifestName = resolver.openInputStream(current)?.bufferedReader()?.use {
                it.readText().trim()
            } ?: return null

            val manifest = db.findChild(resolver, manifestName) ?: return null
            val mtime = manifest.queryLong(resolver, COLUMN_LAST_MODIFIED) ?: return null

            val worldId = this.identifier
            WorkspaceStore.remove(this.appContext, worldId)?.let {
                JvmFile(cacheDir, it.uuid.toString()).deleteRecursively()
            }
            var uuid: UUID
            do {
                uuid = UUID.randomUUID()
                folder = JvmFile(cacheDir, uuid.toString())
            } while (folder.exists())
            db.copyFolderTo(resolver, folder)
            val neo = PreparedWorkspace().apply {
                uuidMostSignificantBits = uuid.mostSignificantBits
                uuidLeastSignificantBits = uuid.leastSignificantBits
                manifestModifiedMillis = mtime
            }
            WorkspaceStore.write(
                this.appContext,
                worldId,
                neo
            )
            return Workspace(uuid, folder)
        } catch (e: IOException) {
            Log.e(LEVEL_DB_TAG, "Failed to open level db at ${folder?.path} from $root", e)
        }
        return null
    }

    override suspend fun merge(leveldb: String): Uri? {
        val resolver = this.appContext.contentResolver
        val db = this.root.findChild(resolver, FOLDER_DATABASE) ?: return null

        val backup = DocumentsContract.renameDocument(
            resolver, db, nextBackupName(resolver)
        ) ?: return null

        val target = DocumentsContract.createDocument(
            resolver, this.root, MIME_TYPE_DIR, FOLDER_DATABASE
        )
        if (target === null) {
            DocumentsContract.renameDocument(resolver, backup, FOLDER_DATABASE)
            return null
        }

        val copied = try {
            JvmFile(leveldb).copyContentsToDocument(resolver, target)
            true
        } catch (e: IOException) {
            Log.e(APP_TAG, "Failed to merge world db at $root", e)
            false
        }

        if (copied) {
            DocumentsContract.deleteDocument(resolver, backup)
        } else {
            DocumentsContract.deleteDocument(resolver, target)
            DocumentsContract.renameDocument(resolver, backup, FOLDER_DATABASE)
        }
        return null
    }

    private fun nextBackupName(resolver: ContentResolver): String {
        var name = "$FOLDER_DATABASE-bak"
        var index = 1
        while (this.root.findChild(resolver, name) !== null) {
            name = "$FOLDER_DATABASE-bak($index)"
            index++
        }
        return name
    }
}