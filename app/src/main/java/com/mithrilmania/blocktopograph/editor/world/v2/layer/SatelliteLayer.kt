package com.mithrilmania.blocktopograph.editor.world.v2.layer

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.set
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.editor.world.v2.CHUNK_INDICES
import com.mithrilmania.blocktopograph.map.Biome
import com.mithrilmania.blocktopograph.util.ColorUtil
import com.mithrilmania.blocktopograph.util.Noise
import com.mithrilmania.blocktopograph.world.chunk.BrightnessSource
import com.mithrilmania.blocktopograph.world.chunk.Chunk
import com.mithrilmania.blocktopograph.world.chunk.ChunkCache
import com.mithrilmania.blocktopograph.world.chunk.ChunkPos
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// shading Amp, possible range: [0, 2] (or use negative for reverse shading)
private const val shadingAmp = 0.8f

fun getHeightShading(height: Int, heightW: Int, heightN: Int): Float {
    var samples = 0
    var heightDiff = 0f

    if (heightW > 0) {
        heightDiff += (height - heightW).toFloat()
        samples++
    }

    if (heightN > 0) {
        heightDiff += (height - heightN).toFloat()
        samples++
    }

    heightDiff *= 1.05.pow(samples.toDouble()).toFloat()

    // emphasize small differences in height, but as the difference in height increases, don't increase so much
    return ((atan(heightDiff.toDouble()) / Math.PI).toFloat() * shadingAmp) + 1f
}

fun ChunkPos.getNoise(x: Int, z: Int): Int {
    // noise values are between -1 and 1
    // 0.0001 is added to the coordinates because integer values result in 0
    val xval = (chunkX shl 4 or x).toDouble()
    val zval = (chunkZ shl 4 or z).toDouble()
    val oct1 = Noise.noise(
        (xval / 100.0) % 256 + 0.0001,
        (zval / 100.0) % 256 + 0.0001
    )
    val oct2 = Noise.noise(
        (xval / 20.0) % 256 + 0.0001,
        (zval / 20.0) % 256 + 0.0001
    )
    val oct3 = Noise.noise(
        (xval / 3.0) % 256 + 0.0001,
        (zval / 3.0) % 256 + 0.0001
    )
    return (60 + (40 * oct1) + (14 * oct2) + (6 * oct3)).toInt()
}

//calculate color of one column
suspend fun getColumnColor(
    chunk: Chunk,
    x: Int,
    top: Int,
    z: Int,
    heightW: Int,
    heightN: Int
): Int {
    val bottom = chunk.lowerBound
    var y = top
    var alphaRemain = 1.0F
    var finalR = 0f
    var finalG = 0f
    var finalB = 0f
    val context = currentCoroutineContext()
    while (y >= bottom && alphaRemain >= 0.1F) {
        if (!context.isActive) return 0
        val blockTemplate = chunk.getBlock(x, y, z)

        if (BlockTemplates.getAirTemplate() == blockTemplate) {
            y--
            continue  //skip air blocks
        }

        val color = blockTemplate.color

        // no need to process block if it is fully transparent
        if (Color.alpha(color) == 0) {
            y--
            continue
        }

        val blendA = Color.alpha(color) / 255f

        // alpha blend and multiply
        var blendR = alphaRemain * blendA * (Color.red(color) / 255f)
        var blendG = alphaRemain * blendA * (Color.green(color) / 255f)
        var blendB = alphaRemain * blendA * (Color.blue(color) / 255f)

        //blend biome-colored blocks
        if (blockTemplate.isHasBiomeShading) {
            val biome = Biome.getBiome(chunk.getBiome(x, y, z) and 0xff)
            val noise: Int = chunk.pos.getNoise(x, z)
            val color = biome.color
            val r = 30 + (Color.red(color) / 5) + noise
            val g = 110 + (Color.green(color) / 5) + noise
            val b = 30 + (Color.blue(color) / 5) + noise
            val grassColor = ColorUtil.truncateRgb(r, g, b)
            blendR *= Color.red(grassColor).toFloat() / 255f
            blendG *= Color.green(grassColor).toFloat() / 255f
            blendB *= Color.blue(grassColor).toFloat() / 255f
        }

        finalR += blendR
        finalG += blendG
        finalB += blendB
        alphaRemain *= 1f - blendA
        y--
    }

    //height shading (based on slopes in terrain; height diff)
    val heightShading = getHeightShading(y, heightW, heightN)

    //go back to "surface"
    y++
    //light sources
    val lightValue = chunk.getBrightness(BrightnessSource.BLOCK, x, y, z) and 0xff
    val lightShading = lightValue.toFloat() / 15f + 1

    //mix shading
    val shading = heightShading * lightShading

    //low places just get darker
    //shading *= Math.max(Math.min(y / 40f, 1f), 0.2f);//shade ravines & caves, minimum *0.2 to keep some color

    // apply the shading
    finalR = min(max(0f, finalR * shading), 1f)
    finalG = min(max(0f, finalG * shading), 1f)
    finalB = min(max(0f, finalB * shading), 1f)


    // now we have our final RGB values as floats, convert to a packed ARGB pixel.
    return -0x1000000 or
            ((((finalR * 255f).toInt()) and 0xff) shl 16) or
            ((((finalG * 255f).toInt()) and 0xff) shl 8) or
            (((finalB * 255f).toInt()) and 0xff)
}

suspend fun renderSatellite(
    bitmap: Bitmap,
    cache: ChunkCache,
    chunk: Chunk,
    left: Int,
    top: Int
) {
    val pos = chunk.pos
    val chunkX = pos.chunkX
    val chunkZ = pos.chunkZ
    val dataW = cache.getAsync(pos.copy(chunkX = chunkX - 1))
    val dataN = cache.getAsync(pos.copy(chunkZ = chunkZ - 1))
    var tY: Int = top + CHUNK_INDICES
    for (z in CHUNK_INDICES downTo 0) {
        var tX: Int = left + CHUNK_INDICES
        for (x in CHUNK_INDICES downTo 0) {
            val y: Int = chunk.getTop(x, z)
            val color = getColumnColor(
                chunk,
                x,
                y,
                z,
                if (x == 0) {
                    dataW.await()?.getTop(CHUNK_INDICES, z) ?: y //chunk edge
                } else chunk.getTop(x - 1, z),  //within chunk
                if (z == 0) {
                    dataN.await()?.getTop(x, CHUNK_INDICES) ?: y //chunk edge
                } else chunk.getTop(x, z - 1) //within chunk
            )
            bitmap[tX, tY] = color
            --tX
        }
        --tY
    }
}