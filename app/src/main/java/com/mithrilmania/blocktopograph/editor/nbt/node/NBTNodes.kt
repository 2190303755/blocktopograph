package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.PrimitiveTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.util.NBTStringifier
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts

private val UID: AtomicInteger = AtomicInteger()

interface RootLike {
    val canBeHeterogeneous: Boolean
    val depth: Int
    fun makePath(child: String): String
}

sealed class NBTNode(
    val parent: RootLike,
    key: Any
) {
    val uid: Int = UID.getAndIncrement()
    val depth: Int = parent.depth + 1
    var key: Any by mutableStateOf(key)
    val path: String by derivedStateOf {
        parent.makePath(this.key.toString())
    }
    abstract val type: Byte
    abstract var expanded: Boolean
    abstract val children: Collection<NBTNode>
    abstract fun toBinaryTag(): BinaryTag
    @Composable
    abstract fun icon(): Painter

    @Composable
    abstract fun summary(): String

    @Composable
    abstract fun Editor(editor: NBTEditorModel)
}

sealed class RootNode(
    parent: RootLike,
    key: Any
) : NBTNode(parent, key), RootLike {
    override var expanded: Boolean by mutableStateOf(parent !is RootNode)
    abstract fun remove(key: Any): NBTNode?
    abstract fun insert(key: Any, node: NBTNode): Boolean
    abstract fun replace(key: Any, node: NBTNode): Boolean
}

sealed class ValueNode(
    parent: RootLike,
    key: Any
) : NBTNode(parent, key) {
    override var expanded: Boolean
        set(_) {}
        get() = false
    override val children: Collection<NBTNode> get() = emptyList()
    abstract override fun toBinaryTag(): PrimitiveTag

    @Composable
    override fun Editor(editor: NBTEditorModel) {

    }
}

sealed class CollectionNode<N : NBTNode, T : BinaryTag>(
    parent: RootLike,
    key: Any,
    tag: T?
) : RootNode(parent, key) {
    override val children: MutableList<N> = if (tag === null) {
        mutableStateListOf()
    } else {
        this.buildNodes(tag)
    }

    abstract override fun toBinaryTag(): T
    abstract fun cast(node: NBTNode): N?
    abstract fun buildNodes(tag: T): MutableList<N>
    override fun insert(key: Any, node: NBTNode): Boolean {
        if (key is Number) {
            val value = this.cast(node)
            if (value !== null) {
                var index = key.toInt()
                this.children.add(index, value)
                val iterator = this.children.listIterator(++index)
                while (iterator.hasNext()) {
                    iterator.next().key = index++
                }
                return true
            }
        }
        return false
    }

    override fun replace(key: Any, node: NBTNode): Boolean {
        if (key is Number) {
            val index = key.toInt()
            if (index in 0 until this.children.size) {
                val value = this.cast(node)
                if (value !== null) {
                    this.children[index] = value
                    return true
                }
            }
        }
        return false
    }

    override fun remove(key: Any): NBTNode? {
        if (key is Number) {
            var index = key.toInt()
            if (index in 0 until children.size) {
                val node = this.children.removeAt(index)
                val iterator = this.children.listIterator(index)
                while (iterator.hasNext()) {
                    iterator.next().key = index++
                }
                return node
            }
        }
        return null
    }

    fun swap(a: Int, b: Int): Boolean {
        if (a < 0 || b < 0) return false
        val nodes = this.children
        if (maxOf(a, b) >= nodes.size) return false
        val temp = nodes[a]
        nodes[a] = nodes[b]
        nodes[a].key = a
        nodes[b] = temp
        temp.key = b
        return true
    }
}

fun NBTNode.visit(action: (NBTNode) -> Unit) {
    action(this)
    if (this.expanded) {
        this.children.forEach {
            it.visit(action)
        }
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
fun BinaryTag.buildNode(
    parent: RootLike,
    key: Any
): NBTNode = when (this) {
    is CompoundTag -> MapNode(parent, key, this)
    is ByteArrayTag -> ByteArrayNode(parent, key, this)
    is IntArrayTag -> IntArrayNode(parent, key, this)
    is LongArrayTag -> LongArrayNode(parent, key, this)
    is ListTag -> ListNode(parent, key, this)
    is ByteTag -> ByteNode(parent, key, this.value)
    is ShortTag -> ShortNode(parent, key, this.value)
    is IntTag -> IntNode(parent, key, this.value)
    is LongTag -> LongNode(parent, key, this.value)
    is FloatTag -> FloatNode(parent, key, this.value)
    is DoubleTag -> DoubleNode(parent, key, this.value)
    is StringTag -> StringNode(parent, key, this.value)
    else -> throw IllegalArgumentException()
}

fun NBTNode.stringify(heterogeneous: Boolean = true): String {
    val stringifier: NBTStringifier
    if (this.parent is ListNode) {
        stringifier = NBTStringifier(heterogeneous = heterogeneous)
    } else {
        val name = this.key.toString()
        stringifier = NBTStringifier(
            heterogeneous = heterogeneous,
            builder = StringBuilder(name.length + 32)
                .appendSafeLiteral(name)
                .append(':')
                .append(' ')
        )
    }
    this.toBinaryTag().accept(stringifier)
    return stringifier.toString()
}
