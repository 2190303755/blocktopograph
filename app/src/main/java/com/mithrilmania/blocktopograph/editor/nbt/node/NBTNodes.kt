package com.mithrilmania.blocktopograph.editor.nbt.node

import android.content.Context
import android.view.MenuItem
import com.mithrilmania.blocktopograph.editor.nbt.Insert
import com.mithrilmania.blocktopograph.editor.nbt.NBTTree
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.editor.nbt.insertNode
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.EndTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.util.NBTStringifier
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
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
    fun isSame(node: NBTNode): Boolean
    fun asTag(): BinaryTag<*>
    fun registerAs(parent: RootNode<*>, key: String): NBTNode
    fun getChildren(): Collection<NBTNode>
}

sealed interface RootNode<K> : NBTNode {
    val tree: NBTTree
    fun remove(key: K): NBTNode?
    fun insert(key: K, node: NBTNode): Boolean
    fun makeInsertOption(item: MenuItem, context: Context)
    override fun isSame(node: NBTNode) = this === node
}

interface MapNode : RootNode<String> {
    fun containsKey(key: String): Boolean
    fun put(key: String, node: NBTNode): NBTNode?
    override fun asTag(): CompoundTag
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
    ) = parent.tree.register(key) { uid, name -> CompoundNode(parent, uid, name, null) }
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

fun Map<String, BinaryTag<*>>?.registerTo(
    parent: RootNode<*>
): ConcurrentSkipListMap<String, NBTNode> {
    if (this === null) return ConcurrentSkipListMap()
    val nodes = ConcurrentSkipListMap<String, NBTNode>()
    this.forEach { (key, tag) ->
        if (tag !== EndTag) {
            nodes[key] = tag.registerTo(parent, key)
        }
    }
    return nodes
}

fun BinaryTag<*>.registerTo(
    parent: RootNode<*>,
    key: String
): NBTNode = parent.tree.register(key) { uid, name ->
    when (this) {
        is CompoundTag -> CompoundNode(parent, uid, name, this)
        is ByteArrayTag -> ByteArrayNode(parent, uid, name, this)
        is IntArrayTag -> IntArrayNode(parent, uid, name, this)
        is LongArrayTag -> LongArrayNode(parent, uid, name, this)
        is ListTag -> TagListNode(parent, uid, name, this)
        is ByteTag -> ByteNode(parent, uid, name, this.value)
        is ShortTag -> ShortNode(parent, uid, name, this.value)
        is IntTag -> IntNode(parent, uid, name, this.value)
        is LongTag -> LongNode(parent, uid, name, this.value)
        is FloatTag -> FloatNode(parent, uid, name, this.value)
        is DoubleTag -> DoubleNode(parent, uid, name, this.value)
        is StringTag -> StringNode(parent, uid, name, this.value)
        else -> throw IllegalArgumentException()
    }
}

fun NBTNode.stringify(): String {
    if (this.parent is ListNode) {
        val builder = NBTStringifier()
        this.asTag().accept(builder)
        return builder.toString()
    }
    val name = this.name
    val builder = NBTStringifier(
        StringBuilder(name.length + 64)
            .appendSafeLiteral(name)
            .append(':')
            .append(' ')
    )
    this.asTag().accept(builder)
    return builder.toString()
}