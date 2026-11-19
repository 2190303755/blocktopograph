package com.mithrilmania.blocktopograph.marker

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.FilterQuality
import coil3.size.Size
import com.mithrilmania.blocktopograph.ASSETS_URL_PREFIX
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.util.SizedImage
import com.mithrilmania.blocktopograph.world.Dimension

val DEFAULT_MARKER_ICON_SIZE = Size(48, 48)

fun markerIcon(data: String) =
    SizedImage(data, DEFAULT_MARKER_ICON_SIZE, FilterQuality.None)

fun markerIcon(@DrawableRes data: Int) =
    SizedImage(data, DEFAULT_MARKER_ICON_SIZE, FilterQuality.Low)

/**
 * 地图上的一个标记：只描述「是什么、在哪、用什么图标」，不携带 View / Bitmap。
 * 过渡期仅迁入出生点与本地玩家两个内置标记。
 */
sealed class MapMarker(
    @JvmField val x: Int,
    @JvmField val z: Int,
    @JvmField val dimension: Dimension,
    @JvmField val icon: SizedImage
) {

    /** 出生点标记。 */
    class Spawn(
        x: Int,
        z: Int,
        dimension: Dimension
    ) : MapMarker(x, z, dimension, markerIcon(R.drawable.outline_home_pin))

    class Player(
        private val identifier: ByteArray?,
        x: Int,
        z: Int,
        dimension: Dimension
    ) : MapMarker(
        x,
        z,
        dimension,
        markerIcon(ASSETS_URL_PREFIX + "items/empty_armor_slot_helmet.png")
    ) {
        val isLocal: Boolean get() = this.identifier === null
        // TODO impl NBTSource
    }

    class Custom(
        @JvmField val name: String,
        x: Int,
        z: Int,
        dimension: Dimension,
        icon: SizedImage
    ) : MapMarker(x, z, dimension, icon)
}
