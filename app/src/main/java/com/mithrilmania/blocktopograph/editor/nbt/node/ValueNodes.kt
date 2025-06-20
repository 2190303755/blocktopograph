package com.mithrilmania.blocktopograph.editor.nbt.node

import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE
import com.mithrilmania.blocktopograph.nbt.TAG_DOUBLE
import com.mithrilmania.blocktopograph.nbt.TAG_FLOAT
import com.mithrilmania.blocktopograph.nbt.TAG_INT
import com.mithrilmania.blocktopograph.nbt.TAG_LONG
import com.mithrilmania.blocktopograph.nbt.TAG_SHORT
import com.mithrilmania.blocktopograph.nbt.TAG_STRING
import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtDouble
import net.benwoodworth.knbt.NbtFloat
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtLong
import net.benwoodworth.knbt.NbtShort
import net.benwoodworth.knbt.NbtString
import java.lang.ref.WeakReference

sealed class ValueNode<T>(
    override val parent: RootNode<*>,
    override val uid: Int,
    name: String,
    var value: T
) : NBTNode {
    override var holder: WeakReference<NodeHolder<*, *>>? = null
    override val expanded get() = false
    override val depth = parent.depth + 1
    override fun getChildren() = emptySet<NBTNode>()
    override fun hashCode() = this.uid
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is ValueNode<*> && this.value == other.value
    }
    override var name: String = name
        set(value) {
            field = value
            this.holder?.get()?.onRename(value)
        }
}

class ByteNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Byte
) : ValueNode<Byte>(parent, uid, name, data) {
    override val type get() = TAG_BYTE
    override fun asTag() = NbtByte(this.value)
    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> ByteNode(parent, uid, name, 0) }
}

class ShortNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Short
) : ValueNode<Short>(parent, uid, name, data) {
    override val type get() = TAG_SHORT
    override fun asTag() = NbtShort(this.value)
    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> ShortNode(parent, uid, name, 0) }
}

class IntNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Int
) : ValueNode<Int>(parent, uid, name, data) {
    override val type get() = TAG_INT
    override fun asTag() = NbtInt(this.value)
    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> IntNode(parent, uid, name, 0) }
}

class LongNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Long
) : ValueNode<Long>(parent, uid, name, data) {
    override val type get() = TAG_LONG
    override fun asTag() = NbtLong(this.value)
    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> LongNode(parent, uid, name, 0) }
}

class FloatNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Float
) : ValueNode<Float>(parent, uid, name, data) {
    override val type get() = TAG_FLOAT
    override fun asTag() = NbtFloat(this.value)
    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> FloatNode(parent, uid, name, 0.0F) }
}

class DoubleNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Double
) : ValueNode<Double>(parent, uid, name, data) {
    override val type get() = TAG_DOUBLE
    override fun asTag() = NbtDouble(this.value)
    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> DoubleNode(parent, uid, name, 0.0) }
}

class StringNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: String
) : ValueNode<String>(parent, uid, name, data) {
    override val type get() = TAG_STRING
    override fun asTag() = NbtString(this.value)
    override fun registerAs(
        parent: RootNode<*>,
        key: String
    ) = parent.tree.register(key) { uid, name -> StringNode(parent, uid, name, "") }
}