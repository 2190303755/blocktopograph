package com.mithrilmania.blocktopograph.world

import android.content.res.Resources
import androidx.annotation.StringRes
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readNamedTag
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.io.ByteArrayInputStream

sealed interface Dimension {
    val identifier: String
    val runtimeId: Int

    fun getDisplayName(res: Resources): String
}

data class CustomDimension(
    override val identifier: String,
    override val runtimeId: Int,
) : Dimension {
    override fun getDisplayName(res: Resources): String = this.identifier
}

enum class VanillaDimension(
    override val identifier: String,
    @JvmField @field:StringRes val display: Int
) : Dimension {
    OVERWORLD("Overworld", R.string.overworld),
    NETHER("Nether", R.string.nether),
    END("End", R.string.the_end);

    override val runtimeId: Int get() = this.ordinal
    override fun getDisplayName(res: Resources): String = res.getString(this.display)
}

val Dimension.isOverworld: Boolean get() = this.runtimeId == 0
val VANILLA_DIMENSIONS: Int2ObjectMap<VanillaDimension> = run {
    val dimensions = Int2ObjectOpenHashMap<VanillaDimension>(3)
    dimensions[0] = VanillaDimension.OVERWORLD
    dimensions[1] = VanillaDimension.NETHER
    dimensions[2] = VanillaDimension.END
    dimensions
}

fun buildDimensionRegistry(serialized: ByteArray? = null): Int2ObjectMap<out Dimension> {
    if (serialized !== null) {
        val dimensions = (BedrockNBTInput(
            ByteArrayInputStream(serialized)
        ).readNamedTag().second as? CompoundTag)?.get("entries")
        if (dimensions is CompoundTag) {
            val registry = Int2ObjectOpenHashMap<Dimension>(dimensions.size + 3)
            registry.putAll(VANILLA_DIMENSIONS)
            for (pair in dimensions) {
                val id = (pair.value as? NumericTag ?: continue).toInt()
                registry.put(id, CustomDimension(pair.key, id))
            }
            return registry
        }
    }
    return VANILLA_DIMENSIONS
}

fun Dimension.defaultMapTypeCompat(): MapType = when (this.runtimeId) {
    0 -> MapType.OVERWORLD_SATELLITE
    1 -> MapType.NETHER
    2 -> MapType.END_SATELLITE
    else -> MapType.ERROR
}
