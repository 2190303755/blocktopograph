package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import androidx.annotation.Keep
import com.mithrilmania.blocktopograph.IFileService
import com.mithrilmania.blocktopograph.IWorldCallback
import com.mithrilmania.blocktopograph.ParcelFileMetadata
import com.mithrilmania.blocktopograph.util.runSuppressing
import com.mithrilmania.blocktopograph.util.size
import com.mithrilmania.blocktopograph.world.FILE_BEHAVIOR_PACKS
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.FILE_RESOURCE_PACKS
import com.mithrilmania.blocktopograph.world.FOLDER_DATABASE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.system.exitProcess

class FileService() : IFileService.Stub() {
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Constructor with Context. This is only available from Shizuku API v13.
     * <p>
     * This method need to be annotated with {@link Keep} to prevent ProGuard from removing it.
     *
     * @param context Context created with createPackageContextAsUser
     * @see <a href="https://github.com/RikkaApps/Shizuku-API/blob/672f5efd4b33c2441dbf609772627e63417587ac/server-shared/src/main/java/rikka/shizuku/server/UserService.java#L66">code used to create the instance of this class</a>
     */
    @Suppress("unused")
    @Keep
    constructor (context: Context) : this()

    override fun destroy() {
        this.coroutineScope.cancel()
        exitProcess(0)
    }

    override fun exit() {
        this.coroutineScope.cancel()
        exitProcess(0)
    }

    override fun loadWorlds(path: String, callback: IWorldCallback): Boolean {
        val root = File(path)
        if (!root.isDirectory) return false
        this.coroutineScope.launch {
            root.listFiles(File::isDirectory)?.forEach { folder ->
                val config = File(folder, FILE_LEVEL_DAT)
                if (!config.isFile) return@forEach
                val folderPath = folder.absolutePath
                var datFd: ParcelFileDescriptor? = null
                try {
                    datFd = ParcelFileDescriptor.open(
                        config,
                        ParcelFileDescriptor.MODE_READ_WRITE
                    )
                    callback.onWorldSubmit(folderPath, datFd, null)
                } catch (_: RemoteException) {
                    datFd?.close()
                }
                val behaviors = async {
                    val file = File(folder, FILE_BEHAVIOR_PACKS)
                    if (file.isFile) {
                        runSuppressing {
                            FileInputStream(file).bufferedReader().use {
                                return@async JSONArray(it.readText()).length()
                            }
                        }
                    }
                    return@async 0
                }
                val resources = async {
                    val file = File(folder, FILE_RESOURCE_PACKS)
                    if (file.isFile) {
                        runSuppressing {
                            FileInputStream(file).bufferedReader().use {
                                return@async JSONArray(it.readText()).length()
                            }
                        }
                    }
                    return@async 0
                }
                val size = async { folder.size }
                launch {
                    callback.onStatisticsUpdate(
                        folderPath,
                        behaviors.await(),
                        resources.await(),
                        size.await()
                    )
                }
            }
        }
        return true
    }

    override fun copyTo(src: String, dest: String) {
        // TODO: check args
        File(src).copyRecursively(File(dest))
    }


    override fun prepareDB(cache: String, world: String): String? {
        if (!File(cache).isDirectory) return null
        val src = File(world, FOLDER_DATABASE)
        if (!src.isDirectory) return null
        var folder: File
        do {
            folder = File(cache, UUID.randomUUID().toString())
        } while (folder.exists())
        src.copyRecursively(folder)
        return folder.absolutePath
    }

    override fun canonicalize(path: String): String? {
        return File(path).canonicalFile.takeIf(File::exists)?.path
    }

    override fun metadata(path: String): ParcelFileMetadata? {
        val nioPath = Paths.get(path)
        val attrs = try {
            Files.readAttributes(
                nioPath,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )
        } catch (_: NoSuchFileException) {
            return null
        } catch (_: FileSystemException) {
            return null
        }

        return ParcelFileMetadata().apply {
            isRegularFile = attrs.isRegularFile
            isDirectory = attrs.isDirectory
            symlinkTarget = if (attrs.isSymbolicLink) {
                Files.readSymbolicLink(nioPath).toString()
            } else {
                null
            }
            size = attrs.size()
            createdAtMillis = attrs.creationTime()?.toMillis() ?: 0L
            lastModifiedAtMillis = attrs.lastModifiedTime()?.toMillis() ?: 0L
            lastAccessedAtMillis = attrs.lastAccessTime()?.toMillis() ?: 0L
        }
    }

    override fun openReadOnly(path: String): ParcelFileDescriptor? {
        return File(path).takeIf(File::isFile)
            ?.open(ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun openReadWrite(
        path: String,
        mustCreate: Boolean,
        mustExist: Boolean
    ): ParcelFileDescriptor? {
        val file = File(path)
        require(!mustCreate || !mustExist) {
            "Cannot require mustCreate and mustExist at the same time."
        }
        if (mustCreate && file.exists()) return null
        if (mustExist && !file.isFile) return null
        return file.open(ParcelFileDescriptor.MODE_READ_WRITE)
    }

    override fun createDirectory(path: String): Boolean {
        return File(path).mkdir()
    }

    override fun atomicMove(source: String, target: String): Boolean {
        runSuppressing {
            Files.move(Paths.get(source), Paths.get(target), ATOMIC_MOVE, REPLACE_EXISTING)
            return true
        }
        return false
    }

    override fun delete(path: String): Boolean {
        return File(path).delete()
    }
}