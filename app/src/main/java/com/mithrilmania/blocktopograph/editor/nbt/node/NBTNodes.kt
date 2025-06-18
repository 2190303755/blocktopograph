package com.mithrilmania.blocktopograph.editor.nbt.node

import com.mithrilmania.blocktopograph.nbt.appendSafeLiteral
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
import java.util.TreeMap

interface NBTNode {
    val parent: RootNode<*>?
    var name: String
    val uid: Int
    val depth: Int
    val expanded: Boolean
    fun getLayout(): Int
    fun asTag(): NbtTag
    suspend fun getChildren(): Collection<NBTNode>
    abstract override fun equals(other: Any?): Boolean
}

sealed interface RootNode<K> : NBTNode
interface MapNode : RootNode<String> {
    fun remove(key: String): NBTNode?
    fun put(key: String, node: NBTNode): NBTNode?
}

interface ListNode : RootNode<Int> {
    fun remove(node: NBTNode): Boolean
}

interface NodeRegistry {
    fun <T : NBTNode> register(key: String, factory: (Int, String) -> T): T
}

suspend fun NBTNode.visit(
    full: Boolean = false,
    visitor: suspend (NBTNode) -> Unit
) {
    visitor(this)
    if (full || this.expanded) {
        this.getChildren().forEach {
            it.visit(full, visitor)
        }
    }
}

fun Map<String, NbtTag>.registerTo(
    parent: RootNode<*>,
    registry: NodeRegistry
): TreeMap<String, NBTNode> = TreeMap<String, NBTNode>().also {
    this.forEach { (key, tag) ->
        it[key] = tag.registerTo(registry, parent, key)
    }
}

fun NbtTag.registerTo(
    registry: NodeRegistry,
    parent: RootNode<*>,
    key: String
): NBTNode = registry.register(key) { uid, name ->
    when (this) {
        is NbtCompound -> CompoundNode(parent, uid, name, this, registry)
        is NbtByteArray -> ByteArrayNode(parent, uid, name, this, registry)
        is NbtIntArray -> IntArrayNode(parent, uid, name, this, registry)
        is NbtLongArray -> LongArrayNode(parent, uid, name, this, registry)
        is NbtList<*> -> TagListNode(parent, uid, name, this, registry)
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
    return StringBuilder(16 + value.length + name.length)
        .appendSafeLiteral(name)
        .append(": ")
        .append(value)
        .toString()
}

const val LAYOUT_BYTE = 1
const val LAYOUT_SHORT = 2
const val LAYOUT_INT = 3
const val LAYOUT_LONG = 4
const val LAYOUT_FLOAT = 5
const val LAYOUT_DOUBLE = 6
const val LAYOUT_BYTE_ARRAY = 7
const val LAYOUT_STRING = 8
const val LAYOUT_LIST = 9
const val LAYOUT_COMPOUND = 10
const val LAYOUT_INT_ARRAY = 11
const val LAYOUT_LONG_ARRAY = 12
const val LAYOUT_ROOT = 13