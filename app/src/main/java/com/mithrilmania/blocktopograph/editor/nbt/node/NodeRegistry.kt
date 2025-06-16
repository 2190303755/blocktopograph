package com.mithrilmania.blocktopograph.editor.nbt.node

fun interface NodeRegistry {
    fun register(key: String, factory: (Int, String) -> NBTNode): NBTNode
}