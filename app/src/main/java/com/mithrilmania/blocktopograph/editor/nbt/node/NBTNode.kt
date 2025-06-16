package com.mithrilmania.blocktopograph.editor.nbt.node

interface NBTNode {
    val name: String
    val uid: Int
    val depth: Int
    val expanded: Boolean
    fun getLayout(): Int
    suspend fun getChildren(): Collection<NBTNode>
    abstract override fun equals(other: Any?): Boolean
}