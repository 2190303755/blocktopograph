package com.mithrilmania.blocktopograph.editor.nbt.node

import android.content.Context
import android.view.MenuItem
import com.mithrilmania.blocktopograph.editor.nbt.Insert
import com.mithrilmania.blocktopograph.editor.nbt.NBTTree
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.editor.nbt.insertNode
import com.mithrilmania.blocktopograph.nbt.util.EMPTY_COMPOUND
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import kotlinx.serialization.encodeToString
import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtByteArray
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtDouble
import net.benwoodworth.knbt.NbtFloat
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtIntArray
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtLong
import net.benwoodworth.knbt.NbtLongArray
import net.benwoodworth.knbt.NbtShort
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.StringifiedNbt
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentSkipListMap

interface NBTNode {
    val parent: RootNode<*>?
    var holder: WeakReference<NodeHolder<*, *>>?
    var name: String
    val uid: Int
    val type: Int
    val depth: Int
    val expanded: Boolean
    fun asTag(): NbtTag
    fun registerAs(parent: RootNode<*>, key: String): NBTNode
    fun getChildren(): Collection<NBTNode>
    abstract override fun equals(other: Any?): Boolean
}

sealed interface RootNode<K> : NBTNode {
    val tree: NBTTree
    fun remove(key: K): NBTNode?
    fun insert(key: K, node: NBTNode): Boolean
    fun makeInsertOption(item: MenuItem, context: Context)
}

interface MapNode : RootNode<String> {
    fun containsKey(key: String): Boolean
    fun put(key: String, node: NBTNode): NBTNode?
    override fun makeInsertOption(item: MenuItem, context: Context) {
        item.setOnMenuItemClickListener {
            context.insertNode(this) callback@{ node ->
                node.name.let {
                    if (this.containsKey(it)) return@callback
                    this.put(it, node)
                    this.tree.model += Insert(node, this, it)
                    if (this.expanded) {
                        this.tree.reloadAsync()
                    }
                }
            }
            true
        }
    }

    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> CompoundNode(parent, uid, name, EMPTY_COMPOUND) }
}

interface ListNode : RootNode<Int> {
    val size: Int
    override fun getChildren(): List<NBTNode>
    fun indexOf(node: NBTNode): Int
    fun isValid(node: NBTNode): Boolean
    fun swap(left: Int, right: Int): Boolean
}

fun NBTNode.visit(
    full: Boolean = false,
    visitor: (NBTNode) -> Unit
) {
    visitor(this)
    if (full || this.expanded) {
        this.getChildren().forEach {
            it.visit(full, visitor)
        }
    }
}

fun Map<String, NbtTag>.registerTo(
    parent: RootNode<*>
): ConcurrentSkipListMap<String, NBTNode> = ConcurrentSkipListMap<String, NBTNode>().also {
    this.forEach { (key, tag) ->
        it[key] = tag.registerTo(parent, key)
    }
}

fun NbtTag.registerTo(
    parent: RootNode<*>,
    key: String
): NBTNode = parent.tree.register(key) { uid, name ->
    when (this) {
        is NbtCompound -> CompoundNode(parent, uid, name, this)
        is NbtByteArray -> ByteArrayNode(parent, uid, name, this)
        is NbtIntArray -> IntArrayNode(parent, uid, name, this)
        is NbtLongArray -> LongArrayNode(parent, uid, name, this)
        is NbtList<*> -> TagListNode(parent, uid, name, this)
        is NbtByte -> ByteNode(parent, uid, name, this.value)
        is NbtShort -> ShortNode(parent, uid, name, this.value)
        is NbtInt -> IntNode(parent, uid, name, this.value)
        is NbtLong -> LongNode(parent, uid, name, this.value)
        is NbtFloat -> FloatNode(parent, uid, name, this.value)
        is NbtDouble -> DoubleNode(parent, uid, name, this.value)
        is NbtString -> StringNode(parent, uid, name, this.value)
    }
}

fun NBTNode.stringify(): String {
    val value = StringifiedNbt {
        prettyPrint = true
    }.encodeToString(this.asTag())
    if (this.parent is ListNode) return value
    val name = this.name
    return StringBuilder(16 + value.length + name.length).apply {
        appendSafeLiteral(name)
    }.append(": ")
        .append(value)
        .toString()
}