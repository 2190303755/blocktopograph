package com.mithrilmania.blocktopograph.world

import android.content.res.Resources
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.renderer.MapType

interface Dimension { // TODO: sealed
    val name: String
    val id: Int

    fun getDisplayName(res: Resources): String
}

data class CustomDimension(
    override val name: String,
    override val id: Int,
) : Dimension {
    override fun getDisplayName(res: Resources): String = this.name
}

sealed interface VanillaDimension : Dimension {
    object Overworld : VanillaDimension {
        override val name: String get() = "Overworld"
        override val id: Int get() = 0
        override fun getDisplayName(res: Resources): String {
            return res.getString(R.string.overworld)
        }
    }

    object Nether : VanillaDimension {
        override val name: String get() = "Nether"
        override val id: Int get() = 1
        override fun getDisplayName(res: Resources): String {
            return res.getString(R.string.nether)
        }
    }

    object End : VanillaDimension {
        override val name: String get() = "End"
        override val id: Int get() = 2
        override fun getDisplayName(res: Resources): String {
            return res.getString(R.string.the_end)
        }
    }
}

val Dimension.isOverworld: Boolean get() = this.id == 0

fun Dimension.defaultMapTypeCompat(): MapType = when (this.id) {
    0 -> MapType.OVERWORLD_SATELLITE
    1 -> MapType.NETHER
    2 -> MapType.END_SATELLITE
    else -> MapType.ERROR
}
