package com.mithrilmania.blocktopograph.storage

import android.content.Context
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import com.mithrilmania.blocktopograph.IFileService
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_BASIC_WORLD_INFO
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_EXTRA_WORLD_INFO
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_RELEASE
import com.mithrilmania.blocktopograph.util.size
import com.mithrilmania.blocktopograph.world.FILE_BEHAVIOR_PACKS
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.FILE_RESOURCE_PACKS
import com.mithrilmania.blocktopograph.world.FILE_WORLD_ICON
import com.mithrilmania.blocktopograph.world.FOLDER_DATABASE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.UUID
import kotlin.system.exitProcess

class FileServiceServer() : IFileService.Stub() {
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
        exitProcess(0)
    }

    override fun exit() {
        exitProcess(0)
    }

    override fun loadWorldsAsync(path: String, messenger: Messenger) {
        val root = File(path)
        if (!root.isDirectory) return
        CoroutineScope(Dispatchers.IO).launch {
            val jobs = ArrayList<Deferred<*>>()
            root.listFiles { it.isDirectory }?.forEach {
                val config = File(it, FILE_LEVEL_DAT)
                if (!config.isFile) return@forEach
                val icon = File(it, FILE_WORLD_ICON)
                val path = it.absolutePath
                messenger.send(Message.obtain().apply {
                    what = CODE_BASIC_WORLD_INFO
                    data = Bundle().apply {
                        putString("Path", path)
                        putParcelable(
                            "Dat", ParcelFileDescriptor.open(
                                config,
                                ParcelFileDescriptor.MODE_READ_WRITE
                            )
                        )
                        if (icon.isFile) {
                            putParcelable(
                                "Icon",
                                ParcelFileDescriptor.open(
                                    icon,
                                    ParcelFileDescriptor.MODE_READ_ONLY
                                )
                            )
                        }
                    }
                })
                jobs.add(async(Dispatchers.IO) {
                    val behavior = async(Dispatchers.IO) {
                        val file = File(it, FILE_BEHAVIOR_PACKS)
                        if (file.isFile) {
                            BufferedReader(InputStreamReader(FileInputStream(file))).use {
                                return@async JSONArray(it.readText()).length()
                            }
                        }
                        return@async 0
                    }
                    val resource = async(Dispatchers.IO) {
                        val file = File(it, FILE_RESOURCE_PACKS)
                        if (file.isFile) {
                            BufferedReader(InputStreamReader(FileInputStream(file))).use {
                                return@async JSONArray(it.readText()).length()
                            }
                        }
                        return@async 0
                    }
                    val size = async(Dispatchers.IO) { it.size }
                    messenger.send(Message.obtain().apply {
                        what = CODE_EXTRA_WORLD_INFO
                        arg1 = behavior.await()
                        arg2 = resource.await()
                        data = Bundle().apply {
                            putString("Path", path)
                            putLong("Size", size.await())
                        }
                    })
                })
            }
            jobs.forEach { it.await() }
            messenger.send(Message.obtain().apply { what = CODE_RELEASE })
        }
    }

    override fun copyTo(src: String, dest: String) {
        File(src).copyRecursively(File(dest))
    }

    override fun getFileDescriptor(path: String): ParcelFileDescriptor? {
        val file = File(path)
        return if (file.isFile) ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_WRITE
        ) else null
    }

    override fun prepareDB(cache: String, world: String): String? {
        // runBlocking
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
}