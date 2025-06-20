package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Handler.Callback
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Size
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.nbt.tags.LongTag
import com.mithrilmania.blocktopograph.nbt.tags.StringTag
import com.mithrilmania.blocktopograph.storage.FileServiceClient
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_BASIC_WORLD_INFO
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_EXTRA_WORLD_INFO
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_RELEASE
import com.mithrilmania.blocktopograph.storage.ShizukuLocation
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.getGameMode
import com.mithrilmania.blocktopograph.util.getSafely
import com.mithrilmania.blocktopograph.util.lastPlayedVersion
import com.mithrilmania.blocktopograph.util.loadThumbnail
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_TIME
import com.mithrilmania.blocktopograph.world.KEY_LEVEL_NAME
import com.mithrilmania.blocktopograph.world.KEY_RANDOM_SEED
import com.mithrilmania.blocktopograph.world.WorldInfo
import com.mithrilmania.blocktopograph.worldlist.WorldListModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentLinkedQueue

private val callbacks = ConcurrentLinkedQueue<Callback>()
fun loadShizukuWorlds(
    model: WorldListModel,
    context: Context,
    location: String,
    tag: String = ""
) {
    val service = Blocktopograph.fileService ?: return
    val adapter = model.adapter
    model.loading.postValue(true)
    val jobs = HashMap<String, CompletableDeferred<WorldInfo>>()
    val callback = object : Callback {
        override fun handleMessage(msg: Message): Boolean {
            when (msg.what) {
                CODE_RELEASE -> callbacks.remove(this)
                CODE_BASIC_WORLD_INFO -> {
                    val bundle = msg.data
                    val path = bundle.getString("Path") ?: return true
                    val icon: ParcelFileDescriptor?
                    val config: ParcelFileDescriptor
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        @Suppress("DEPRECATION")
                        config = bundle.getParcelable("Dat")
                            ?: return true
                        @Suppress("DEPRECATION")
                        icon = bundle.getParcelable("Icon")
                    } else {
                        config = bundle.getSafely("Dat") ?: return true
                        icon = bundle.getSafely("Icon")
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        val res = async(Dispatchers.IO) {
                            if (icon == null) null else if (
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                            ) {
                                BitmapFactory.decodeStream(FileInputStream(icon.fileDescriptor))
                            } else icon.loadThumbnail(context.resources.let {
                                Size(
                                    it.getDimensionPixelSize(R.dimen.large_world_icon_width),
                                    it.getDimensionPixelSize(R.dimen.large_world_icon_height)
                                )
                            })
                        }
                        val compound = LevelDataConverter.read(
                            FileInputStream(config.fileDescriptor)
                        )
                        val world = WorldInfo(
                            ShizukuLocation(path),
                            ShizukuLocation("$path/$FILE_LEVEL_DAT"),
                            (compound?.getChildTagByKey(KEY_LEVEL_NAME) as? StringTag)?.value
                                ?: File(path).name,
                            compound.getGameMode(context),
                            (compound?.getChildTagByKey(KEY_LAST_PLAYED_TIME) as? LongTag)?.value?.let {
                                it * 1000L
                            } ?: 0L,
                            (compound?.getChildTagByKey(KEY_RANDOM_SEED) as? LongTag)?.value?.toString()
                                ?: "",
                            compound.lastPlayedVersion,
                            tag
                        )
                        withContext(Dispatchers.Main) {
                            adapter.worlds.add(world)
                        }
                        world.icon = res.await()
                        synchronized(jobs) {
                            jobs.computeIfAbsent(path) {
                                CompletableDeferred()
                            }.complete(world)
                        }
                        config.close()
                    }
                }

                CODE_EXTRA_WORLD_INFO -> {
                    val bundle = msg.data
                    val path = bundle.getString("Path") ?: return true
                    val size = ConvertUtil.formatSize(bundle.getLong("Size"))
                    val behavior = msg.arg1
                    val resource = msg.arg2
                    CoroutineScope(Dispatchers.Default).launch {
                        var job: Deferred<WorldInfo>
                        synchronized(jobs) {
                            job = jobs.computeIfAbsent(path) {
                                CompletableDeferred()
                            }
                        }
                        val world = job.await()
                        world.size = size
                        world.behavior = behavior
                        world.resource = resource
                        withContext(Dispatchers.Main) {
                            adapter.notifyItemChanged(world)
                        }
                    }
                }
            }
            return true
        }
    }
    callbacks.add(callback)
    service.loadWorldsAsync(location, Messenger(Handler(FileServiceClient.looper, callback)))
    model.loading.postValue(false)
}