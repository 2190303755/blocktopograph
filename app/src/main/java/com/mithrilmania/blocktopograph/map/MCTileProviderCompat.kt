package com.mithrilmania.blocktopograph.map

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.chunk.Chunk
import com.mithrilmania.blocktopograph.editor.world.WorldViewerModel
import com.mithrilmania.blocktopograph.map.locator.getMarkerManager
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.nbt.CollectionTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.util.resolveVec3f
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.runSuppressing
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun loadEntityMarkers(
    dimension: Dimension,
    chunk: Chunk,
    markers: MutableList<AbstractMarker>
) {
    try {
        val entityData = chunk.entity ?: return
        entityData.load()
        for (tag in entityData.tags.values) {
            if (tag !is CompoundTag) continue
            val entity: Entity = tag.getTyped<StringTag>("identifier")?.let {
                Entity.getEntity(it.value)
            } ?: tag.getTyped<IntTag>("id")?.let {
                Entity.getEntity(it.value)
            } ?: Entity.UNKNOWN

            val pos = tag.getTyped<CollectionTag<*>>("Pos")?.resolveVec3f()
            markers.add(
                AbstractMarker(
                    pos?.x?.roundToInt() ?: 0,
                    pos?.y?.roundToInt() ?: 0,
                    pos?.z?.roundToInt() ?: 0,
                    dimension,
                    entity,
                    false
                )
            )
        }
    } catch (e: Exception) {
        Log.w(APP_TAG, "Error when loading entity markers", e)
    }
}

fun loadBlockEntityMarkers(
    dimension: Dimension,
    chunk: Chunk,
    markers: MutableList<AbstractMarker>
) {
    try {
        val tileEntityData = chunk.blockEntity ?: return
        tileEntityData.load()
        for (tag in tileEntityData.tags.values) {
            if (tag !is CompoundTag) continue
            val id = tag["id"] as? StringTag ?: continue
            val te = TileEntity.getTileEntity(id.value)
            if (te?.bitmap == null) continue
            markers.add(
                AbstractMarker(
                    tag.getTyped<NumericTag>("x")?.toInt() ?: 0,
                    tag.getTyped<NumericTag>("y")?.toInt() ?: 0,
                    tag.getTyped<NumericTag>("z")?.toInt() ?: 0,
                    dimension,
                    te,
                    false
                )
            )
        }
    } catch (e: Exception) {
        Log.w(APP_TAG, "Error when loading block entity markers", e)
    }
}

fun loadCustomMarkers(
    world: World,
    chunkX: Int,
    chunkZ: Int,
    markers: MutableList<AbstractMarker>
) {
    runSuppressing {
        markers.addAll(world.getMarkerManager().getMarkersOfChunk(chunkX, chunkZ))
    }
}

fun WorldModel.loadMarkers(
    map: WorldViewerModel,
    minChunkX: Int,
    minChunkZ: Int,
    maxChunkX: Int,
    maxChunkZ: Int,
    dimension: Dimension
) {
    this.viewModelScope.launch(Dispatchers.IO) {
        val storage = this@loadMarkers.world.storage ?: return@launch
        val markers = mutableListOf<AbstractMarker>()
        for (chunkX in minChunkX until maxChunkX) {
            for (chunkZ in minChunkZ until maxChunkZ) {
                markers.clear()
                val chunk = storage.getChunk(chunkX, chunkZ, dimension)
                loadEntityMarkers(dimension, chunk, markers)
                loadBlockEntityMarkers(dimension, chunk, markers)
                loadCustomMarkers(this@loadMarkers.world, chunkX, chunkZ, markers)
                map.pendingMarkers.send(markers)
            }
        }
    }
}