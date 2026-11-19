package com.mithrilmania.blocktopograph.world.edit

import android.util.Log
import androidx.compose.ui.unit.IntRect
import com.mithrilmania.blocktopograph.map.edit.EditResultCode
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.world.CHUNK_DIMENSION
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.chunk.Chunk
import com.mithrilmania.blocktopograph.world.chunk.ChunkCache
import com.mithrilmania.blocktopograph.world.chunk.ChunkPos
import com.mithrilmania.blocktopograph.world.chunk.SubChunk

/**
 * Iterates every chunk or block position inside a block-coordinate rectangle, mirroring the old
 * RectEditTarget but built on the read/write Kotlin chunk stack
 * ([com.mithrilmania.blocktopograph.world.chunk.Chunk]).
 */
class SelectionEditTarget(
    private val cache: ChunkCache,
    dimension: Dimension,
    private val area: IntRect
) {
    private val dimensionId = dimension.runtimeId

    private var maxError = 10

    fun setMaxError(maxError: Int) {
        this.maxError = maxError
    }

    fun forEachChunk(
        action: (chunk: Chunk, fromX: Int, toX: Int, fromZ: Int, toZ: Int) -> Int
    ): EditResultCode = this.forEach(chunkAction = action, blockAction = null)

    fun forEachXz(action: (chunk: Chunk, x: Int, z: Int) -> Int): EditResultCode =
        this.forEach(chunkAction = null, blockAction = action)

    fun forEachXyz(action: (chunk: Chunk, x: Int, y: Int, z: Int) -> Int): EditResultCode =
        this.forEach(chunkAction = null, blockAction = { chunk, x, z ->
            var count = 0
            for (y in chunk.lowerBound until chunk.upperBound) {
                count += action(chunk, x, y, z)
            }
            count
        })

    fun forEachXYZSubChunk(action: (subChunk: SubChunk, x: Int, y: Int, z: Int) -> Int): EditResultCode =
        this.forEachChunk { chunk, fromX, toX, fromZ, toZ ->
            var count = 0
            for (y in chunk.lowerBound until chunk.upperBound step CHUNK_DIMENSION) {
                val subChunk = chunk.getSubChunk(y) ?: continue
                for (z in fromZ..toZ) {
                    for (x in fromX..toX) {
                        repeat(16) { localY ->
                            count += action(subChunk, x, localY, z)
                        }
                    }
                }
            }
            count
        }

    private fun forEach(
        chunkAction: ((Chunk, Int, Int, Int, Int) -> Int)?,
        blockAction: ((Chunk, Int, Int) -> Int)?
    ): EditResultCode {
        var exceptionCount = 0

        val left = this.area.left
        val top = this.area.top
        val right = this.area.right - 1
        val bottom = this.area.bottom - 1

        val chunkMinX = left shr 4
        val chunkMaxX = right shr 4
        val chunkMinZ = top shr 4
        val chunkMaxZ = bottom shr 4

        for (chunkX in chunkMinX..chunkMaxX) {
            val innerMinX = if (chunkX == chunkMinX) left and 0xF else 0
            val innerMaxX = if (chunkX == chunkMaxX) right and 0xF else 15

            for (chunkZ in chunkMinZ..chunkMaxZ) {
                val innerMinZ = if (chunkZ == chunkMinZ) top and 0xF else 0
                val innerMaxZ = if (chunkZ == chunkMaxZ) bottom and 0xF else 15

                val pos = ChunkPos(this.dimensionId, chunkX, chunkZ)
                // Chunks not present in the database have nothing to edit or delete.
                val chunk = this.cache[pos] ?: continue

                if (chunkAction !== null) {
                    val result = try {
                        chunkAction(
                            chunk,
                            innerMinX, innerMaxX,
                            innerMinZ, innerMaxZ
                        )
                    } catch (e: Exception) {
                        Log.w(APP_TAG, "Chunk edit failed at ($chunkX,$chunkZ)", e)
                        -1
                    }
                    if (result != 0) {
                        exceptionCount = this.recordError(
                            exceptionCount,
                            "Failed with chunk ($chunkX,$chunkZ), code $result"
                        )
                        if (exceptionCount > this.maxError) return EditResultCode.QUIT_TOO_MANY_ERROR
                    }
                } else if (blockAction !== null) {
                    for (x in innerMinX..innerMaxX) {
                        for (z in innerMinZ..innerMaxZ) {
                            val result = blockAction(chunk, x, z)
                            if (result != 0) {
                                exceptionCount = this.recordError(
                                    exceptionCount,
                                    "Failed at ($chunkX,$chunkZ), rel ($x,$z), code $result"
                                )
                                if (exceptionCount > this.maxError) {
                                    return EditResultCode.QUIT_TOO_MANY_ERROR
                                }
                            }
                        }
                    }
                }

                try {
                    chunk.save()
                } catch (e: Exception) {
                    exceptionCount = this.recordError(
                        exceptionCount,
                        "Failed to save chunk ($chunkX,$chunkZ)"
                    )
                    Log.w(APP_TAG, "Failed to save chunk ($chunkX,$chunkZ)", e)
                    if (exceptionCount > this.maxError) return EditResultCode.QUIT_TOO_MANY_ERROR
                }
                // Make sure the renderer and later edits reload the modified chunk.
                this.cache.invalidate(pos)
            }
        }
        return if (exceptionCount > 0) EditResultCode.PARTIALLY_FAILED else EditResultCode.SUCCESS
    }

    private fun recordError(count: Int, message: String): Int {
        if (count < 5) Log.w(APP_TAG, message)
        return count + 1
    }
}
