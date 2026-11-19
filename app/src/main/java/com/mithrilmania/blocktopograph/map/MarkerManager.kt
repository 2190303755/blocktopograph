package com.mithrilmania.blocktopograph.map

import android.content.Context
import android.util.LongSparseArray
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.block.KnownBlockRepr
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.marker.CustomNamedBitmapProvider
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.util.NamedBitmapProvider
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.VanillaDimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.regex.Pattern

class MarkerManager(private val markerFile: File, context: Context) {
    private val appContext: Context = context.applicationContext

    private var markerSet: MutableSet<AbstractMarker> = HashSet()
    private val chunks = LongSparseArray<MutableSet<AbstractMarker>>()

    private var dirty = false

    fun listMarkers(): List<AbstractMarker> {
        return ArrayList(markerSet)
    }

    private fun xzToKey(x: Int, z: Int): Long = (x.toLong() shl 32) or z.toLong()

    fun removeMarker(marker: AbstractMarker, dirty: Boolean) {
        val chunkKey = xzToKey(marker.x shr 4, marker.z shr 4)
        chunks.get(chunkKey)?.remove(marker)

        // only set it to dirty if the marker is removed.
        this.dirty = this.dirty or (markerSet.remove(marker) && dirty)
    }

    fun addMarker(marker: AbstractMarker, dirty: Boolean) {
        markerSet.add(marker)

        val chunkKey = xzToKey(marker.x shr 4, marker.z shr 4)
        var chunk = chunks.get(chunkKey)
        if (chunk == null) {
            chunk = HashSet()
            chunks.put(chunkKey, chunk)
        }
        chunk.add(marker)

        this.dirty = this.dirty or dirty
    }

    suspend fun loadFromFile() {
        markerSet = HashSet()

        if (runCatching { markerFile.isPresent(appContext) }.getOrDefault(false)) {
            try {
                markerFile.read(appContext) { input ->
                    input.bufferedReader().use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            if (line.length < 6) {
                                line = reader.readLine()
                                continue
                            }
                            try {
                                when (val format = line.split(" ")[0].toInt()) {
                                    // add versions here if it changes
                                    else -> {
                                        val matcher = formatRegex.matcher(line)
                                        if (matcher.find()) {
                                            val markerDisplayName =
                                                matcher.group(2).replace("\\\"", "\"")
                                            val iconName = matcher.group(3).replace("\\\"", "\"")
                                            val x = matcher.group(4).toInt()
                                            val y = matcher.group(5).toInt()
                                            val z = matcher.group(6).toInt()
                                            val dimensionName =
                                                matcher.group(7).replace("\\\"", "\"")
                                            val dimension = when (dimensionName) {
                                                "nether" -> VanillaDimension.NETHER
                                                "end" -> VanillaDimension.END
                                                else -> VanillaDimension.OVERWORLD
                                            }
                                            addMarker(
                                                markerFromData(
                                                    markerDisplayName,
                                                    iconName,
                                                    x,
                                                    y,
                                                    z,
                                                    dimension
                                                ),
                                                false
                                            )
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                LogUtil.d(this, "Invalid line in marker file: $line")
                            }
                            line = reader.readLine()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        this.dirty = false
    }

    /**
     * Saves the current markers, if and only if the markers were changed.
     */
    fun saveIfNeeded(owner: LifecycleOwner) {
        if (dirty) {
            owner.lifecycleScope.launch(Dispatchers.IO) {
                saveToFile()
                dirty = false
            }
        }
    }

    suspend fun saveToFile() {
        try {
            markerFile.save(appContext) { output ->
                PrintWriter(BufferedWriter(OutputStreamWriter(output))).use { out ->
                    for (marker in markerSet) {
                        out.format(
                            "1 \"%s\", \"%s\", %d %d %d %s ;\n",
                            marker.namedBitmapProvider.bitmapDisplayName
                                .replace("\"", "\\\""),
                            marker.namedBitmapProvider.bitmapDataName
                                .replace("\"", "\\\""),
                            marker.x, marker.y, marker.z,
                            marker.dimension.identifier.lowercase()
                                .replace("\"", "\\\"")
                        )
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getMarkersOfChunk(chunkX: Int, chunkZ: Int): Set<AbstractMarker> {
        val key = xzToKey(chunkX, chunkZ)
        var chunk = chunks.get(key)
        if (chunk == null) {
            chunk = HashSet()
            chunks.put(key, chunk)
        }
        return chunk
    }

    companion object {

        // g0: Whole match
        // g1: Format version (int)
        // g2: Marker display name (escaped string)
        // g3: Marker icon name (escaped string)
        // g4: X coordinate (int)
        // g5: Y coordinate (int)
        // g6: Z coordinate (int)
        // g7: Dimension name (escaped string)
        //
        // example:
        // 1 "Test \" marker" "default \" \"_ma 1 rker 1" 1234 64 4321 overworld ;
        // Why bother inventing a format?
        private val formatRegex = Pattern.compile(
            "(\\d+)\\s+\"(?:(?:(.+?[^\\\\]))|)\"\\s+\"(?:(?:(.+?[^\\\\]))|)\"\\s+(\\d+?)\\s+(\\d+?)\\s+(\\d+?)\\s+(.+?)\\s*;"
        )

        @JvmStatic
        fun markerFromData(
            displayName: String,
            iconName: String,
            x: Int,
            y: Int,
            z: Int,
            dimension: Dimension
        ): AbstractMarker {
            var nbp: NamedBitmapProvider? = KnownBlockRepr.getByDataName(iconName)
            if (nbp == null || nbp.bitmap == null) nbp = Entity.getEntity(iconName)
            if (nbp == null || nbp.bitmap == null) nbp = TileEntity.getTileEntity(iconName)
            if (nbp == null || nbp.bitmap == null) nbp = CustomIcon.getCustomIcon(iconName)
            if (nbp == null || nbp.bitmap == null) nbp = CustomIcon.DEFAULT_MARKER

            return AbstractMarker(
                x, y, z, dimension,
                CustomNamedBitmapProvider(nbp, displayName), true
            )
        }
    }
}