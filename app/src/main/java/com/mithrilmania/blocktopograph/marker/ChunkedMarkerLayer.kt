package com.mithrilmania.blocktopograph.marker

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.mithrilmania.blocktopograph.util.tryOrNull
import com.mithrilmania.blocktopograph.world.CHUNK_DIMENSION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.cameraX
import ovh.plrapps.mapcompose.api.cameraY
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapModel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.floor

/**
 * 区块化 marker 的轻量描述。图标是「已解码、可直接绘制」的位图（不再携带 @Composable），
 * 由 [ChunkMarkerSource] 在加载区块时（通常经 Coil 3）解码后返回。
 * 标识符用静态 [AtomicLong] 自增，仅作进程内唯一句柄，不追求跨会话稳定。
 */
class ChunkMarker(
    @JvmField val worldX: Int,
    @JvmField val worldZ: Int,
    @JvmField val icon: ImageBitmap,
    val quality: FilterQuality,
    @JvmField val zIndex: Float = 0f,
) {
    val id: Long = nextId.incrementAndGet()

    companion object {
        private val nextId = AtomicLong()
    }
}

/** 区块 marker 数据源：按（维度, 区块）返回该区块内的所有 marker。 */
fun interface ChunkMarkerSource {
    suspend fun loadChunk(
        context: Context,
        dimensionId: Int,
        chunkX: Int,
        chunkZ: Int,
    ): List<ChunkMarker>
}

/** 可见区块 AABB（含预加载 padding）。 */
data class VisibleChunks(
    val startX: Int,
    val endX: Int,
    val startZ: Int,
    val endZ: Int,
) {
    val isEmpty: Boolean get() = startX > endX || startZ > endZ

    companion object {
        val EMPTY = VisibleChunks(0, -1, 0, -1)
    }
}

/** 区块标识（含维度，避免跨维度同名坐标串扰）。 */
private data class ChunkKey(val dimensionId: Int, val chunkX: Int, val chunkZ: Int)

/**
 * 区块加载 / 卸载状态机：
 *  - 视口变化时 diff 掉「滑出视口」的区块、异步补齐「进入视口」的区块；
 *  - [onDimensionChanged] 切换维度时使 in-flight 结果作废并清空。
 */
class ChunkedMarkerState(
    context: Context,
    private val source: ChunkMarkerSource,
) {
    private val appContext: Context = context.applicationContext
    /**
     * 可见区块：(维度, 区块) -> 该区块内的 marker 列表（[ChunkMarkerSource] 返回后不再修改）。
     * 用 [ConcurrentHashMap] 承载，绘制端对其值视图做「弱一致遍历」，无需加锁，
     * 也不会像 [androidx.compose.runtime.snapshots.SnapshotStateList] 那样抛 ConcurrentModificationException。
     */
    private val loaded = ConcurrentHashMap<ChunkKey, List<ChunkMarker>>()

    /** 正在加载的区块。`newKeySet().add()` 的布尔返回值天然完成 check-and-set，无需额外锁。 */
    private val loading = ConcurrentHashMap.newKeySet<ChunkKey>()

    /** 内容计数：每次增删自增，绘制端读取它以订阅重绘（Compose 状态允许从后台线程写入）。 */
    var revision by mutableIntStateOf(0)
        private set

    /** 维度代际，切换维度时自增使 in-flight 结果作废。 */
    private val generation = AtomicLong()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    /**
     * 绘制视图。getter 内读取 [revision]，因此 draw lambda 调用它即可订阅内容变化；
     * 返回的是 map 的值视图，支持弱一致遍历。
     */
    val markers: Collection<List<ChunkMarker>>
        get() {
            @Suppress("UNUSED_EXPRESSION")
            revision
            return loaded.values
        }

    fun onDimensionChanged() {
        job?.cancel()
        generation.incrementAndGet()
        loaded.clear()
        loading.clear()
        revision++
    }

    fun updateViewport(dimensionId: Int, chunks: VisibleChunks) {
        if (chunks.isEmpty) return
        job?.cancel()
        job = scope.launch {
            val gen = generation.get()
            val target = HashSet<ChunkKey>()
            for (cx in chunks.startX..chunks.endX) {
                for (cz in chunks.startZ..chunks.endZ) {
                    target += ChunkKey(dimensionId, cx, cz)
                }
            }

            // 卸载：滑出视口的区块
            var changed = false
            for (key in loaded.keys.toList()) {
                if (key !in target && loaded.remove(key) != null) changed = true
            }
            if (changed) revision++

            // 加载：缺失区块
            for (key in target) {
                if (loaded.containsKey(key) || !loading.add(key)) continue
                launch {
                    try {
                        val ms = source.loadChunk(
                            appContext,
                            key.dimensionId,
                            key.chunkX,
                            key.chunkZ,
                        )
                        if (gen == generation.get() && key in target) {
                            loaded[key] = ms
                            revision++
                        }
                    } catch (_: Throwable) {
                        // 区块加载失败则跳过，等待下次视口变化重试
                    } finally {
                        loading.remove(key)
                    }
                }
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }
}

/**
 * 高性能区块化 marker 层：单个 [Canvas] 批量绘制当前可见区块内的 marker，
 * 替换「每个 marker 一个 Composable」的默认方案。
 */
@Composable
fun ChunkedMarkerLayer(
    model: MapModel,
    dimensionId: Int,
    source: ChunkMarkerSource,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    val context = LocalContext.current.applicationContext
    val state = remember(source, context) { ChunkedMarkerState(context, source) }
    DisposableEffect(state) { onDispose { state.dispose() } }

    // 切换维度：清空并令 in-flight 结果作废
    LaunchedEffect(dimensionId) { state.onDimensionChanged() }

    val iconPx = LocalDensity.current.run { iconSize.toPx().toInt() }

    // camera / scale / 布局尺寸变化 → 重算可见区块 → 驱动加载/卸载。
    // layoutSize 由 ZoomPan 的 MeasurePolicy 写入模型，无需本层 onSizeChanged。
    LaunchedEffect(dimensionId) {
        snapshotFlow { resolveVisibleChunks(model, padding = 1) }
            .distinctUntilChanged()
            .collect { state.updateViewport(dimensionId, it) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val scale = model.scale.toFloat()
        val camX = model.cameraX.toFloat()
        val camY = model.cameraY.toFloat()
        val half = iconPx / 2f
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f

        for (chunk in state.markers) {
            for (m in chunk) {
                val sx = cx + (m.worldX - camX) * scale
                val sy = cy + (m.worldZ - camY) * scale
                if (sx + half < 0f || sx - half > size.width || sy + half < 0f || sy - half > size.height) {
                    continue
                }
                drawImage(
                    image = m.icon,
                    dstOffset = IntOffset((sx - half).toInt(), (sy - half).toInt()),
                    dstSize = IntSize(iconPx, iconPx),
                    filterQuality = m.quality
                )
            }
        }
    }
}

/** 由 camera / scale / 布局尺寸计算可见区块 AABB（含 padding 圈预加载）。 */
private fun resolveVisibleChunks(
    model: MapModel,
    padding: Int,
): VisibleChunks {
    val scale = model.scale.toFloat()
    if (scale <= 0f) return VisibleChunks.EMPTY
    val layoutSize = model.layoutSize
    if (layoutSize.width == 0 || layoutSize.height == 0) return VisibleChunks.EMPTY
    val camX = model.cameraX.toFloat()
    val camY = model.cameraY.toFloat()
    val cx = layoutSize.width * 0.5f
    val cy = layoutSize.height * 0.5f
    val startX = floor((camX - cx / scale) / CHUNK_DIMENSION).toInt() - padding
    val endX = floor((camX + cx / scale) / CHUNK_DIMENSION).toInt() + padding
    val startZ = floor((camY - cy / scale) / CHUNK_DIMENSION).toInt() - padding
    val endZ = floor((camY + cy / scale) / CHUNK_DIMENSION).toInt() + padding
    return VisibleChunks(startX, endX, startZ, endZ)
}

/** 用 Coil 3 异步解码一个图像源（独立图，非精灵图），返回可绘制的 [ImageBitmap]。 */
suspend fun MapMarker.decodeIcon(context: Context): ImageBitmap? = tryOrNull {
    val loader = SingletonImageLoader.get(context)
    val result = loader.execute(
        ImageRequest.Builder(context)
            .data(this.icon.data)
            .size(this.icon)
            .build()
    )
    (result.image as? BitmapImage)?.bitmap?.asImageBitmap()
}
