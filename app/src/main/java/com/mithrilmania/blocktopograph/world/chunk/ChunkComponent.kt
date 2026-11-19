package com.mithrilmania.blocktopograph.world.chunk

interface ChunkComponent {
    fun save(forced: Boolean = false): ByteArray?
}
