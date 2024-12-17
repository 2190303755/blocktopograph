package com.mithrilmania.blocktopograph.view

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.mithrilmania.blocktopograph.Log
import com.mithrilmania.blocktopograph.map.Dimension
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.util.copyFolderTo
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.iq80.leveldb.Options
import java.io.File
import java.io.IOException
import java.util.UUID

class WorldMapModel : WorldModel() {
    private val caches: ArrayList<File> = arrayListOf()
    val storage: MutableLiveData<WorldStorage> = MutableLiveData<WorldStorage>()
    val markers: MutableLiveData<ArrayList<AbstractMarker>> =
        MutableLiveData<ArrayList<AbstractMarker>>(arrayListOf())

    var dimension: Dimension = Dimension.OVERWORLD

    val worldType: MutableLiveData<MapType> =
        MutableLiveData<MapType>(Dimension.OVERWORLD.defaultMapType)

    val showActionBar: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showGrid: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showDrawer: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val showMarkers: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    override fun init(context: Context, location: Uri?): Boolean {
        if (!super.init(context, location)) return false
        CoroutineScope(Dispatchers.IO).launch {
            var storage: WorldStorage
            try {
                val resolver = context.contentResolver
                val source =
                    this@WorldMapModel.instance?.root?.findChild(resolver, "db") ?: return@launch
                val cache = context.cacheDir
                var folder: File
                do {
                    folder = File(cache, UUID.randomUUID().toString())
                } while (folder.exists())
                source.copyFolderTo(resolver, folder)
                this@WorldMapModel.caches.add(folder);
                storage = WorldStorage(folder.path, Options.newDefaultOptions(), location)
            } catch (e: IOException) {
                Log.e(this@WorldMapModel, e)
                return@launch
            }
            withContext(Dispatchers.Main) {
                this@WorldMapModel.storage.value = storage
            }
        }
        return true
    }

    override fun onCleared() {
        super.onCleared()
        try {
            this.storage.value?.db?.close()
        } catch (_: Throwable) {
        }
        for (folder in this.caches) {
            try {
                folder.deleteRecursively()
            } catch (_: Throwable) {
            }
        }
    }
}