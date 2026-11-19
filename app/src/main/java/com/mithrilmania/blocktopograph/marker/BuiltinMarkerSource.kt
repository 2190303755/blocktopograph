package com.mithrilmania.blocktopograph.marker

import android.content.Context
import com.mithrilmania.blocktopograph.world.CHUNK_DIMENSION

/**
 * 内置标记（出生点、本地玩家）的区块数据源：按维度与区块过滤 [MapMarker]，
 * 在加载区块时把图标解码为可直接绘制的 [ChunkMarker]。
 */
class BuiltinMarkerSource(
    private val markers: List<MapMarker>,
) : ChunkMarkerSource {
    override suspend fun loadChunk(
        context: Context,
        dimensionId: Int,
        chunkX: Int,
        chunkZ: Int,
    ): List<ChunkMarker> {
        val result = ArrayList<ChunkMarker>()
        for (marker in markers) {
            if (marker.dimension.runtimeId != dimensionId) continue
            if (Math.floorDiv(marker.x, CHUNK_DIMENSION) != chunkX) continue
            if (Math.floorDiv(marker.z, CHUNK_DIMENSION) != chunkZ) continue
            val icon = marker.decodeIcon(context) ?: continue
            result += ChunkMarker(marker.x, marker.z, icon, marker.icon.quality)
        }
        return result
    }
}
