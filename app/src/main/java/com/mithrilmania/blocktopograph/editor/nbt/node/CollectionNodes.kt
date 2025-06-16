package com.mithrilmania.blocktopograph.editor.nbt.node

import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtTag
import java.util.TreeMap

sealed class CollectionNode<K, T>(
    val parent: RootNode,
    override var uid: Int,
    override var name: String,
    data: (RootNode) -> T
) : RootNode {
    override var expanded = false
    val data by lazy { data(this) }
    override val depth = parent.depth + 1

    override fun hashCode() = this.uid
    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

class CompoundNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: NbtCompound,
    registry: NodeRegistry
) : CollectionNode<String, TreeMap<String, NBTNode>>(parent, uid, name, {
    data.registerTo(it, registry)
}) {
    override suspend fun getChildren() = this.data.values
    override fun getLayout() = LAYOUT_COMPOUND
}

class ListNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: List<NbtTag>,
    registry: NodeRegistry
) : CollectionNode<String, MutableList<NBTNode>>(parent, uid, name, { self ->
    ArrayList<NBTNode>(data.size).also {
        data.forEachIndexed { index, tag ->
            it.add(tag.registerTo(registry, self, index.toString()))
        }
    }
}) {
    override suspend fun getChildren() = this.data
    override fun getLayout() = LAYOUT_LIST
}

class ByteArrayNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: List<Byte>,
    registry: NodeRegistry
) : CollectionNode<String, MutableList<NBTNode>>(parent, uid, name, { self ->
    ArrayList<NBTNode>(data.size).also {
        data.forEachIndexed { index, value ->
            it.add(registry.register(index.toString()) { uid, name ->
                ByteNode(self, uid, name, value)
            })
        }
    }
}) {
    override suspend fun getChildren() = this.data
    override fun getLayout() = LAYOUT_BYTE_ARRAY
}

class IntArrayNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: List<Int>,
    registry: NodeRegistry
) : CollectionNode<String, MutableList<NBTNode>>(parent, uid, name, { self ->
    ArrayList<NBTNode>(data.size).also {
        data.forEachIndexed { index, value ->
            it.add(registry.register(index.toString()) { uid, name ->
                IntNode(self, uid, name, value)
            })
        }
    }
}) {
    override suspend fun getChildren() = this.data
    override fun getLayout() = LAYOUT_INT_ARRAY
}

class LongArrayNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: List<Long>,
    registry: NodeRegistry
) : CollectionNode<String, MutableList<NBTNode>>(parent, uid, name, { self ->
    ArrayList<NBTNode>(data.size).also {
        data.forEachIndexed { index, value ->
            it.add(registry.register(index.toString()) { uid, name ->
                LongNode(self, uid, name, value)
            })
        }
    }
}) {
    override suspend fun getChildren() = this.data
    override fun getLayout() = LAYOUT_LONG_ARRAY
}