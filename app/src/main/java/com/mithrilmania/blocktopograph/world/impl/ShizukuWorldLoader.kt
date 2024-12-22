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
import androidx.lifecycle.MutableLiveData
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.nbt.tags.LongTag
import com.mithrilmania.blocktopograph.nbt.tags.StringTag
import com.mithrilmania.blocktopograph.storage.FileServiceClient
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_BASIC_WORLD_INFO
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_EXTRA_WORLD_INFO
import com.mithrilmania.blocktopograph.storage.FileServiceClient.CODE_RELEASE
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.getGameMode
import com.mithrilmania.blocktopograph.util.lastPlayedVersion
import com.mithrilmania.blocktopograph.util.loadThumbnail
import com.mithrilmania.blocktopograph.world.IWorldLoader
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_TIME
import com.mithrilmania.blocktopograph.world.KEY_LEVEL_NAME
import com.mithrilmania.blocktopograph.world.KEY_RANDOM_SEED
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.worldlist.WorldItemAdapter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object ShizukuWorldLoader : IWorldLoader<String> {
    private val callbacks = ArrayList<Callback>()
    override suspend fun loadWorlds(
        adapter: WorldItemAdapter,
        context: Context,
        location: String,
        state: MutableLiveData<Boolean>,
        tag: String
    ) {
        val service = Blocktopograph.fileService ?: return
        state.postValue(true)
        val jobs = HashMap<String, CompletableDeferred<ShizukuWorld>>()
        val callback = object : Callback {
            override fun handleMessage(msg: Message): Boolean {
                when (msg.what) {
                    CODE_RELEASE -> callbacks.remove(this)
                    CODE_BASIC_WORLD_INFO -> {
                        val bundle = msg.data
                        val location = bundle.getString("Path") ?: return true
                        var icon: ParcelFileDescriptor?
                        var config: ParcelFileDescriptor
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            @Suppress("DEPRECATION")
                            config = bundle.getParcelable<ParcelFileDescriptor>("Dat")
                                ?: return true
                            @Suppress("DEPRECATION")
                            icon = bundle.getParcelable<ParcelFileDescriptor>("Icon")
                        } else {
                            config = bundle.getParcelable<ParcelFileDescriptor>(
                                "Dat",
                                ParcelFileDescriptor::class.java
                            ) ?: return true
                            icon = bundle.getParcelable<ParcelFileDescriptor>(
                                "Icon",
                                ParcelFileDescriptor::class.java
                            )
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            val icon = async(Dispatchers.IO) {
                                if (icon == null) null else if (
                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                                ) {
                                    BitmapFactory.decodeStream(FileInputStream(icon.fileDescriptor))
                                } else icon.loadThumbnail(context.resources.let {
                                    Size(
                                        it.getDimension(R.dimen.large_world_icon_width).toInt(),
                                        it.getDimension(R.dimen.large_world_icon_height).toInt()
                                    )
                                })
                            }
                            val compound = LevelDataConverter.read(
                                FileInputStream(config.fileDescriptor)
                            )
                            val world = ShizukuWorld(
                                location,
                                (compound?.getChildTagByKey(KEY_LEVEL_NAME) as? StringTag)?.value
                                    ?: File(location).name
                                    ?: context.getString(R.string.default_world_name),
                                compound.getGameMode(context),
                                (compound?.getChildTagByKey(KEY_LAST_PLAYED_TIME) as? LongTag)?.value?.let {
                                    it * 1000L
                                } ?: 0L,
                                (compound?.getChildTagByKey(KEY_RANDOM_SEED) as? LongTag)?.value?.toString()
                                    ?: "",
                                compound.lastPlayedVersion,
                                tag
                            )
                            if (!withContext(Dispatchers.Main) {
                                    adapter.addWorld(world)
                                }) return@launch
                            world.icon = icon.await()
                            synchronized(jobs) {
                                jobs.computeIfAbsent(location) {
                                    CompletableDeferred()
                                }.complete(world)
                            }
                            config.close()
                        }
                    }

                    CODE_EXTRA_WORLD_INFO -> {
                        val bundle = msg.data
                        val location = bundle.getString("Path") ?: return true
                        val size = ConvertUtil.formatSize(bundle.getLong("Size"))
                        val behavior = msg.arg1
                        val resource = msg.arg2
                        CoroutineScope(Dispatchers.Default).launch {
                            var job: Deferred<World<*>>
                            synchronized(jobs) {
                                job = jobs.computeIfAbsent(location) {
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
        state.postValue(false)
    }
}