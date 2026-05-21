package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * The engine of MapCompose. The view-model uses two channels to communicate with the [TileCollector]:
 * * one to send [TileSpec]s (a [SendChannel])
 * * one to receive [TileSpec]s (a [ReceiveChannel])
 *
 * The [TileCollector] encapsulates all the complexity that transforms a [TileSpec] into a [Tile].
 * ```
 *                                              _____________________________________________________________________
 *                                             |                           TileCollector             ____________    |
 *                                  tiles      |                                                    |  ________  |   |
 *              ---------------- [*********] <----------------------------------------------------- | | worker | |   |
 *             |                               |                                                    |  --------  |   |
 *             ↓                               |                                                    |  ________  |   |
 *  _____________________                      |                                   tileSpecs        | | worker | |   |
 * | TileCanvasViewModel |                     |    _____________________  <---- [**********] <---- |  --------  |   |
 *  ---------------------  ----> [*********] ----> | tileCollectorKernel |                          |  ________  |   |
 *                                tileSpecs    |    ---------------------  ----> [**********] ----> | | worker | |   |
 *                                             |                                   tileSpecs        |  --------  |   |
 *                                             |                                                    |____________|   |
 *                                             |                                                      worker pool    |
 *                                             |                                                                     |
 *                                              ---------------------------------------------------------------------
 * ```
 * This architecture is an example of Communicating Sequential Processes (CSP).
 *
 * @author p-lr on 22/06/19
 */
internal class TileCollector(
    private val workerCount: Int,
    private val optimizeForLowEndDevices: Boolean,
    private val tileSize: Int
) {
    @Volatile
    var isIdle: Boolean = true

    /**
     * Sets up the tile collector machinery. The architecture is inspired from
     * [Kotlin Conf 2018](https://www.youtube.com/watch?v=a3agLJQ6vt8).
     * It support back-pressure, and avoids deadlock in CSP taking into account recommendations of
     * this [article](https://medium.com/@elizarov/deadlocks-in-non-hierarchical-csp-e5910d137cc),
     * which is from the same author.
     *
     * @param [tileSpecs] channel of [TileSpec], which capacity should be [Channel.RENDEZVOUS].
     * @param [tilesOutput] channel of [Tile], which should be set as [Channel.RENDEZVOUS].
     */
    suspend fun collectTiles(
        tileSpecs: ReceiveChannel<TileSpec>,
        tilesOutput: SendChannel<Tile>,
        layers: List<LayerFactory>,
    ) = coroutineScope {
        val tilesToDownload = Channel<TileSpec>(capacity = Channel.RENDEZVOUS)
        val tilesDownloadedFromWorker = Channel<TileSpec>(capacity = 1)

        repeat(workerCount) {
            worker(
                tilesToDownload = tilesToDownload,
                tilesDownloaded = tilesDownloadedFromWorker,
                tilesOutput = tilesOutput,
                layers = layers
            )
        }
        tileCollectorKernel(tileSpecs, tilesToDownload, tilesDownloadedFromWorker)
    }

    private fun CoroutineScope.worker(
        tilesToDownload: ReceiveChannel<TileSpec>,
        tilesDownloaded: SendChannel<TileSpec>,
        tilesOutput: SendChannel<Tile>,
        layers: List<LayerFactory>,
    ) = launch(dispatcher) {

        val layerIds = layers.map { it.id }
        val canUseHardwareBitmaps = canUseHardwareBitmaps()

        val canvas = Canvas()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        for (spec in tilesToDownload) {
            if (layers.isEmpty()) {
                tilesDownloaded.send(spec)
                continue
            }

            val resolvedLayers = layers.map { layer ->
                async {
                    ResolvedLayer(
                        layer.tileBitmapProvider.getTileBitmap(spec.row, spec.col, spec.zoom),
                        layer.alpha
                    )
                }
            }.awaitAll()

            val primaryLayerBitmap = resolvedLayers.firstOrNull()?.bitmap ?: run {
                tilesDownloaded.send(spec)
                /* When the decoding failed or if there's nothing to decode, then send back the Tile
                 * just as in normal processing, so that the actor which submits tiles specs to the
                 * collector knows that this tile has been processed and does not immediately
                 * re-sends the same spec. */
                tilesOutput.send(
                    Tile(
                        spec.zoom,
                        spec.row,
                        spec.col,
                        spec.subSample,
                        layerIds,
                        layers.map { it.alpha }
                    )
                )
                null
            } ?: continue // If the decoding of the first layer failed, skip the rest

            if (layers.size > 1) {
                canvas.setBitmap(primaryLayerBitmap)

                for (result in resolvedLayers.drop(1)) {
                    paint.alpha = (255f * result.alpha).toInt()
                    if (result.bitmap == null) continue
                    canvas.drawBitmap(result.bitmap, 0f, 0f, paint)
                }
            }

            val resultBitmap = if (canUseHardwareBitmaps) {
                primaryLayerBitmap.copy(Config.HARDWARE, false)
            } else primaryLayerBitmap

            val tile = Tile(
                spec.zoom,
                spec.row,
                spec.col,
                spec.subSample,
                layerIds,
                layers.map { it.alpha }
            ).apply {
                this.bitmap = resultBitmap
            }
            tilesOutput.send(tile)
            tilesDownloaded.send(spec)
        }
    }

    private fun CoroutineScope.tileCollectorKernel(
        tileSpecs: ReceiveChannel<TileSpec>,
        tilesToDownload: SendChannel<TileSpec>,
        tilesDownloadedFromWorker: ReceiveChannel<TileSpec>,
    ) = launch(Dispatchers.Default) {

        val specsBeingProcessed = mutableListOf<TileSpec>()

        while (true) {
            select {
                tilesDownloadedFromWorker.onReceive {
                    specsBeingProcessed.remove(it)
                    isIdle = specsBeingProcessed.isEmpty()
                }
                tileSpecs.onReceive {
                    if (it !in specsBeingProcessed) {
                        /* Add it to the list of specs being processed */
                        specsBeingProcessed.add(it)
                        isIdle = false

                        /* Now download the tile */
                        tilesToDownload.send(it)
                    }
                }
            }
        }
    }

    /**
     * Attempts to stop all actively executing tasks, halts the processing of waiting tasks.
     */
    fun shutdownNow() {
        executor.shutdownNow()
    }

    /**
     * On Android O+, ART has a more efficient GC and HARDWARE Bitmaps are supported, making
     * Bitmap re-use much less important.
     * However:
     * - a framework issue pre Q requires to wait until GL context is initialized. Otherwise,
     * allocating a hardware Bitmap can cause a native crash.
     * - Allocating a hardware Bitmap involves the creation of a file descriptor. Android O, as well
     * as some P devices, have a maximum of 1024 file descriptors. Android Q+ devices have a much
     * higher limit of fd.
     *
     * To avoid all those issues entirely, we enable HARDWARE Bitmaps on Android Q and above.
     * We don't monitor the file descriptor count because in practice, MapCompose creates a few
     * hundreds of them and they seem to be efficiently recycled.
     */
    private fun canUseHardwareBitmaps(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * When using a [LinkedBlockingQueue], the core pool size mustn't be 0, or the active thread
     * count won't be greater than 1. Previous versions used a [SynchronousQueue], which could have
     * a core pool size of 0 and a growing count of active threads. However, a [Runnable] could be
     * rejected when no thread were available. Starting from kotlinx.coroutines 1.4.0, this cause
     * the associated coroutine to be cancelled. By using a [LinkedBlockingQueue], we avoid rejections.
     */
    private val executor = ThreadPoolExecutor(
        workerCount, workerCount,
        60L, TimeUnit.SECONDS, LinkedBlockingQueue()
    ).apply {
        allowCoreThreadTimeOut(true)
    }
    private val dispatcher = executor.asCoroutineDispatcher()
}

private data class ResolvedLayer(val bitmap: Bitmap?, val alpha: Float)