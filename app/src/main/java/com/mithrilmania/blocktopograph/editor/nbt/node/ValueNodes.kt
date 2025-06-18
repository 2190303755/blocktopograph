package com.mithrilmania.blocktopograph.editor.nbt.node

import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtDouble
import net.benwoodworth.knbt.NbtFloat
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtLong
import net.benwoodworth.knbt.NbtShort
import net.benwoodworth.knbt.NbtString

sealed class ValueNode<T>(
    override val parent: RootNode<*>,
    override val uid: Int,
    override var name: String,
    var value: T
) : NBTNode {
    override val expanded get() = false
    override val depth = parent.depth + 1
    override suspend fun getChildren() = emptySet<NBTNode>()
    override fun hashCode() = this.uid
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is ValueNode<*> && this.value == other.value
    }
}

class ByteNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Byte
) : ValueNode<Byte>(parent, uid, name, data) {
    override fun asTag() = NbtByte(this.value)
    override fun getLayout() = LAYOUT_BYTE
}

class ShortNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Short
) : ValueNode<Short>(parent, uid, name, data) {
    override fun asTag() = NbtShort(this.value)
    override fun getLayout() = LAYOUT_SHORT
}

class IntNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Int
) : ValueNode<Int>(parent, uid, name, data) {
    override fun asTag() = NbtInt(this.value)
    override fun getLayout() = LAYOUT_INT
}

class LongNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Long
) : ValueNode<Long>(parent, uid, name, data) {
    override fun asTag() = NbtLong(this.value)
    override fun getLayout() = LAYOUT_LONG
}

class FloatNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Float
) : ValueNode<Float>(parent, uid, name, data) {
    override fun asTag() = NbtFloat(this.value)
    override fun getLayout() = LAYOUT_FLOAT
}

class DoubleNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: Double
) : ValueNode<Double>(parent, uid, name, data) {
    override fun asTag() = NbtDouble(this.value)
    override fun getLayout() = LAYOUT_DOUBLE
}

class StringNode(
    parent: RootNode<*>,
    uid: Int,
    name: String,
    data: String
) : ValueNode<String>(parent, uid, name, data) {
    override fun asTag() = NbtString(this.value)
    override fun getLayout() = LAYOUT_STRING
}