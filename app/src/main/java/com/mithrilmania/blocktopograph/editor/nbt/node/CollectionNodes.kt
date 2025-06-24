package com.mithrilmania.blocktopograph.editor.nbt.node

import android.content.Context
import android.view.MenuItem
import com.mithrilmania.blocktopograph.editor.nbt.Insert
import com.mithrilmania.blocktopograph.editor.nbt.appendNode
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.nbt.TAG_INT_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_LIST
import com.mithrilmania.blocktopograph.nbt.TAG_LONG_ARRAY
import com.mithrilmania.blocktopograph.util.Unchecked
import net.benwoodworth.knbt.NbtByteArray
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtIntArray
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtLongArray
import net.benwoodworth.knbt.NbtTag
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CopyOnWriteArrayList

sealed class CollectionNode<K, T>(
    override val parent: RootNode<*>,
    override var uid: Int,
    name: String,
    data: (RootNode<K>) -> T
) : RootNode<K> {
    override val tree = parent.tree
    override var holder: WeakReference<NodeHolder<*, *>>? = null
    override var expanded = false
    val data by lazy { data(this) }
    override val depth = parent.depth + 1
    override var name: String = name
        set(value) {
            field = value
            this.holder?.get()?.onRename(value)
        }
    override fun hashCode() = this.uid
    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

class CompoundNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: NbtCompound
) : CollectionNode<String, ConcurrentSkipListMap<String, NBTNode>>(
    parent,
    uid,
    name,
    data::registerTo
), MapNode {
    override val type get() = TAG_COMPOUND
    override fun getChildren() = this.data.values
    override fun asTag() = this.data.collect()
    override fun remove(key: String) = this.data.remove(key)
    override fun containsKey(key: String) = this.data.containsKey(key)
    override fun put(key: String, node: NBTNode) = this.data.put(key, node)
    override fun insert(key: String, node: NBTNode): Boolean {
        if (this.data.containsKey(key)) return false
        this.data.put(key, node)
        return true
    }
}

class TagListNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: List<NbtTag>,
) : CollectionNode<Int, MutableList<NBTNode>>(parent, uid, name, { self ->
    data.toNodes { tag, key -> tag.registerTo(self, key) }
}), ListNode {
    override val size by this.data::size
    override val type get() = TAG_LIST
    override fun getChildren() = this.data
    override fun asTag(): NbtList<NbtTag> = Unchecked.collect<NbtTag>(this.data)
    override fun remove(key: Int) = this.data.removeAt(key)
    override fun indexOf(node: NBTNode) = this.data.indexOf(node)
    override fun swap(left: Int, right: Int) = this.data.swap(left, right)
    override fun isValid(node: NBTNode): Boolean {
        return (this.data.firstOrNull() ?: return false).javaClass.isInstance(node)
    }

    override fun insert(key: Int, node: NBTNode): Boolean {
        if (this.isValid(node)) {
            this.data.add(key, node)
            return true
        }
        return false
    }

    override fun makeInsertOption(item: MenuItem, context: Context) {
        item.setOnMenuItemClickListener callback@{
            val first = this.data.firstOrNull()
            if (first === null) {
                context.appendNode(this) { node ->
                    if (this.isValid(node)) {
                        this.append { node }
                    }
                }
            } else {
                this.append {
                    first.registerAs(this, it.toString())
                }
            }
            true
        }
    }

    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> TagListNode(parent, uid, name, emptyList()) }
}

class ByteArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: List<Byte>
) : CollectionNode<Int, MutableList<ByteNode>>(parent, uid, name, { self ->
    val tree = self.tree
    data.toNodes { value, key ->
        tree.register(key) { uid, name ->
            ByteNode(self, uid, name, value)
        }
    }
}), ListNode {
    override val size by this.data::size
    override val type get() = TAG_BYTE_ARRAY
    override fun getChildren() = this.data
    override fun asTag() = NbtByteArray(this.data.collect(::ByteArray))
    override fun remove(key: Int) = this.data.removeAt(key)
    override fun indexOf(node: NBTNode) = this.data.indexOf(node)
    override fun swap(left: Int, right: Int) = this.data.swap(left, right)
    override fun isValid(node: NBTNode) = node is ByteNode
    override fun insert(key: Int, node: NBTNode): Boolean {
        this.data.add(key, node as? ByteNode ?: return false)
        return true
    }

    override fun makeInsertOption(item: MenuItem, context: Context) {
        item.setOnMenuItemClickListener callback@{
            this.append {
                this.tree.register(it.toString()) { uid, name ->
                    ByteNode(this, uid, name, 0)
                }
            }
            true
        }
    }

    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> ByteArrayNode(parent, uid, name, emptyList()) }
}

class IntArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: List<Int>
) : CollectionNode<Int, MutableList<IntNode>>(parent, uid, name, { self ->
    val tree = self.tree
    data.toNodes { value, key ->
        tree.register(key) { uid, name ->
            IntNode(self, uid, name, value)
        }
    }
}), ListNode {
    override val size by this.data::size
    override val type get() = TAG_INT_ARRAY
    override fun getChildren() = this.data
    override fun asTag() = NbtIntArray(this.data.collect(::IntArray))
    override fun remove(key: Int) = this.data.removeAt(key)
    override fun indexOf(node: NBTNode) = this.data.indexOf(node)
    override fun swap(left: Int, right: Int) = this.data.swap(left, right)
    override fun isValid(node: NBTNode) = node is IntNode
    override fun insert(key: Int, node: NBTNode): Boolean {
        this.data.add(key, node as? IntNode ?: return false)
        return true
    }

    override fun makeInsertOption(item: MenuItem, context: Context) {
        item.setOnMenuItemClickListener callback@{
            this.append {
                this.tree.register(it.toString()) { uid, name ->
                    IntNode(this, uid, name, 0)
                }
            }
            true
        }
    }

    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> IntArrayNode(parent, uid, name, emptyList()) }
}

class LongArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: List<Long>
) : CollectionNode<Int, MutableList<LongNode>>(parent, uid, name, { self ->
    val tree = self.tree
    data.toNodes { value, key ->
        tree.register(key) { uid, name ->
            LongNode(self, uid, name, value)
        }
    }
}), ListNode {
    override val size by this.data::size
    override val type get() = TAG_LONG_ARRAY
    override fun getChildren() = this.data
    override fun asTag() = NbtLongArray(this.data.collect(::LongArray))
    override fun remove(key: Int) = this.data.removeAt(key)
    override fun indexOf(node: NBTNode) = this.data.indexOf(node)
    override fun swap(left: Int, right: Int) = this.data.swap(left, right)
    override fun isValid(node: NBTNode) = node is LongNode
    override fun insert(key: Int, node: NBTNode): Boolean {
        this.data.add(key, node as? LongNode ?: return false)
        return true
    }

    override fun makeInsertOption(item: MenuItem, context: Context) {
        item.setOnMenuItemClickListener callback@{
            this.append {
                this.tree.register(it.toString()) { uid, name ->
                    LongNode(this, uid, name, 0)
                }
            }
            true
        }
    }

    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> LongArrayNode(parent, uid, name, emptyList()) }
}

fun Map<String, NBTNode>.collect() = NbtCompound(
    LinkedHashMap<String, NbtTag>().also {
        this.forEach { (name, node) -> it[name] = node.asTag() }
    }
)

inline fun <E, T> List<ValueNode<E>>.collect(
    factory: (Int, (Int) -> E) -> T
) = factory(this.size) { this[it].value }

inline fun <reified T> MutableList<T>.swap(left: Int, right: Int): Boolean {
    if ((0..this.size).let { left !in it || right !in it }) return false
    val temp = this[left]
    this[left] = this[right]
    this[right] = temp
    return true
}

inline fun <reified T : NBTNode> CollectionNode<Int, MutableList<T>>.append(
    factory: (Int) -> T
) {
    val pos = this.data.size
    val node = factory(pos)
    this.data += node
    this.tree.model += Insert<Int>(node, this, pos)
    if (this.expanded) {
        this.tree.reloadAsync()
    }
}

inline fun <I, T : NBTNode> List<I>.toNodes(
    factory: (I, String) -> T
): CopyOnWriteArrayList<T> {
    val temp = ArrayList<T>(this.size)
    this.forEachIndexed { index, value ->
        temp.add(factory(value, index.toString()))
    }
    return CopyOnWriteArrayList(temp)
}