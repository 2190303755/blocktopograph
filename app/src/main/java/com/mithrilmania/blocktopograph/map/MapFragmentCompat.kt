package com.mithrilmania.blocktopograph.map

import android.graphics.Rect
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.chunk.Version
import com.mithrilmania.blocktopograph.editor.world.WorldViewerModel
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_AREA
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_LENGTH
import com.mithrilmania.blocktopograph.util.readIntLE
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.chunk.ChunkTag
import com.mithrilmania.blocktopograph.world.isOverworld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.iq80.leveldb.ReadOptions
import java.util.concurrent.CopyOnWriteArraySet

data class ViewPort(
    @JvmField val minX: Long,
    @JvmField val maxX: Long,
    @JvmField val minZ: Long,
    @JvmField val maxZ: Long,
    @JvmField val dimension: Dimension
)

fun dummyJob(): Job {
    val job = Job()
    job.complete()
    return job
}

fun retainViewPortMarkers(
    fragment: MapFragment,
    markers: CopyOnWriteArraySet<AbstractMarker>,
    sticky: Set<AbstractMarker>,
    viewport: ViewPort
): Job = fragment.lifecycleScope.launch {
    withContext(Dispatchers.Default) {
        val (minX, maxX, minZ, maxZ, dimension) = viewport
        markers.filter {
            it !in sticky && (it.x !in minX..maxX || it.y !in minZ..maxZ || it.dimension != dimension)
        }
    }.forEach(fragment::removeMarker)
}

fun MapFragment.registerSignalListener(model: WorldViewerModel) {
    this.lifecycleScope.launch {
        model.longPressCenter.collect {
            triggerLongPressAtCenter()
        }
    }
}

suspend fun World.analyzeChunksImpl(dimension: Dimension): Rect {
    val storage = this.storage ?: throw NullPointerException()
    val db = storage.db
    val versionMark = ChunkTag.VERSION.dataID
    val legacyVersionMark = ChunkTag.LEGACY_VERSION.dataID
    var rect: Rect? = null
    val extended = !dimension.isOverworld
    var hasWrongChunks = false
    var hasOldChunks = false
    db.iterator(ReadOptions().fillCache(false)).use { iterator ->
        iterator.seekToFirst()
        var count = 0
        while (iterator.hasNext()) {
            ++count
            if (!currentCoroutineContext().isActive) throw InterruptedException()
            val entry = iterator.next()
            val key = entry.key
            if (key.size != if (extended) 13 else 9) continue
            val mark = key.last()
            if (mark != versionMark && mark != legacyVersionMark) continue
            if (extended && key.readIntLE(8) != dimension.runtimeId) continue
            when (Version.getVersion(entry.value)) {
                Version.ERROR, Version.NULL -> hasWrongChunks = true
                Version.OLD_LIMITED -> hasOldChunks = true
                else -> {
                    val chunkX = key.readIntLE(0)
                    val chunkZ = key.readIntLE(4)
                    if (rect === null) {
                        rect = Rect(chunkX, chunkZ, chunkX, chunkZ)
                    } else {
                        rect.union(chunkX, chunkZ)
                        if (((count and 0x1F) == 0 && rect.width() * rect.height() > PICER_MAX_AREA)
                            || rect.width() > PICER_MAX_LENGTH
                            || rect.height() > PICER_MAX_LENGTH
                        ) break
                    }
                }
            }
        }
    }
    if (rect === null) {
        if (hasWrongChunks) throw IllegalStateException()
        if (hasOldChunks) throw UnsupportedOperationException()
        throw NullPointerException()
    }
    rect.left *= 16
    rect.top *= 16
    rect.right *= 16
    rect.bottom *= 16
    return rect
}

