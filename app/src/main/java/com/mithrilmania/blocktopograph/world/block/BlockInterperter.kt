package com.mithrilmania.blocktopograph.world.block

import android.content.Context
import android.graphics.Bitmap
import com.mithrilmania.blocktopograph.block.icon.loadIconWithCache
import com.mithrilmania.blocktopograph.map.Biome
import java.lang.ref.WeakReference

interface BlockInterpreter {
    fun resolveColor(state: BlockState, biome: Biome?): Int
    fun resolveName(state: BlockState, context: Context): String
    fun resolveIcon(state: BlockState, context: Context): Bitmap?
}

class SimpleInterpreter(
    @JvmField val name: String,
    @JvmField val icon: String,
    @JvmField val color: Int
) : BlockInterpreter {
    private var bitmap: WeakReference<Bitmap>? = null
    override fun resolveColor(state: BlockState, biome: Biome?): Int = this.color
    override fun resolveName(state: BlockState, context: Context): String = this.name

    override fun resolveIcon(state: BlockState, context: Context): Bitmap? {
        return context.loadIconWithCache(this.icon)
    }
}
