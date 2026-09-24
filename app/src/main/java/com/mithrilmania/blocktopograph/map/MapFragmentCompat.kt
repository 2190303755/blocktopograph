package com.mithrilmania.blocktopograph.map

import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.world.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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