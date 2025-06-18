package com.mithrilmania.blocktopograph.editor.nbt.node

import com.mithrilmania.blocktopograph.util.Unchecked
import net.benwoodworth.knbt.NbtByteArray
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtIntArray
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtLongArray
import net.benwoodworth.knbt.NbtTag
import java.util.TreeMap

sealed class CollectionNode<K, T>(
    override val parent: RootNode<*>,
    override var uid: Int,
    override var name: String,
    data: (RootNode<K>) -> T
) : RootNode<K> {
    override var expanded = false
    val data by lazy { data(this) }
    override val depth = parent.depth + 1

    override fun hashCode() = this.uid
    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

class CompoundNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: NbtCompound,
    registry: NodeRegistry
) : CollectionNode<String, TreeMap<String, NBTNode>>(parent, uid, name, {
    data.registerTo(it, registry)
}), MapNode {
    override suspend fun getChildren() = this.data.values
    override fun asTag() = this.data.collect()
    override fun getLayout() = LAYOUT_COMPOUND
    override fun remove(key: String) = this.data.remove(key)
    override fun put(key: String, node: NBTNode) = this.data.put(key, node)
}

class TagListNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: List<NbtTag>,
    registry: NodeRegistry
) : CollectionNode<Int, MutableList<NBTNode>>(parent, uid, name, { self ->
    ArrayList<NBTNode>(data.size).also {
        data.forEachIndexed { index, tag ->
            it.add(tag.registerTo(registry, self, index.toString()))
        }
    }
}), ListNode {
    override suspend fun getChildren() = this.data
    override fun asTag(): NbtList<NbtTag> = Unchecked.collect<NbtTag>(this.data)
    override fun remove(node: NBTNode) = this.data.remove(node)
    override fun getLayout() = LAYOUT_LIST
}

class ByteArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: List<Byte>,
    registry: NodeRegistry
) : CollectionNode<Int, MutableList<ByteNode>>(parent, uid, name, { self ->
    ArrayList<ByteNode>(data.size).also {
        data.forEachIndexed { index, value ->
            it.add(registry.register(index.toString()) { uid, name ->
                ByteNode(self, uid, name, value)
            })
        }
    }
}), ListNode {
    override suspend fun getChildren() = this.data
    override fun asTag() = NbtByteArray(this.data.collect(::ByteArray))
    override fun remove(node: NBTNode) = this.data.remove(node)
    override fun getLayout() = LAYOUT_BYTE_ARRAY
}

class IntArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: List<Int>,
    registry: NodeRegistry
) : CollectionNode<Int, MutableList<IntNode>>(parent, uid, name, { self ->
    ArrayList<IntNode>(data.size).also {
        data.forEachIndexed { index, value ->
            it.add(registry.register(index.toString()) { uid, name ->
                IntNode(self, uid, name, value)
            })
        }
    }
}), ListNode {
    override suspend fun getChildren() = this.data
    override fun asTag() = NbtIntArray(this.data.collect(::IntArray))
    override fun remove(node: NBTNode) = this.data.remove(node)
    override fun getLayout() = LAYOUT_INT_ARRAY
}

class LongArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: List<Long>,
    registry: NodeRegistry
) : CollectionNode<Int, MutableList<LongNode>>(parent, uid, name, { self ->
    ArrayList<LongNode>(data.size).also {
        data.forEachIndexed { index, value ->
            it.add(registry.register(index.toString()) { uid, name ->
                LongNode(self, uid, name, value)
            })
        }
    }
}), ListNode {
    override suspend fun getChildren() = this.data
    override fun asTag() = NbtLongArray(this.data.collect(::LongArray))
    override fun remove(node: NBTNode) = this.data.remove(node)
    override fun getLayout() = LAYOUT_LONG_ARRAY
}

fun TreeMap<String, NBTNode>.collect() = NbtCompound(
    LinkedHashMap<String, NbtTag>().also {
        this.forEach { (name, node) -> it[name] = node.asTag() }
    }
)

inline fun <E, T> List<ValueNode<E>>.collect(
    factory: (Int, (Int) -> E) -> T
) = factory(this.size) { this[it].value }

