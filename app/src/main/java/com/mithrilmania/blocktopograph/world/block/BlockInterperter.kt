package com.mithrilmania.blocktopograph.world.block

import android.content.Context
import com.mithrilmania.blocktopograph.map.Biome

interface BlockInterpreter {
    fun resolveColor(state: BlockState, biome: Biome?): Int
    fun resolveName(state: BlockState, context: Context): String
    fun resolveIcon(state: BlockState): Any?
}

class SimpleInterpreter(
    @JvmField val name: String,
    @JvmField val icon: String,
    @JvmField val color: Int
) : BlockInterpreter {
    override fun resolveColor(state: BlockState, biome: Biome?): Int = this.color
    override fun resolveName(state: BlockState, context: Context): String = this.name
    override fun resolveIcon(state: BlockState): String = this.icon
}
