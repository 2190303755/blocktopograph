package com.mithrilmania.blocktopograph.editor.nbt.node

sealed class ValueNode<T>(
    val parent: RootNode,
    override val uid: Int,
    override var name: String,
    var data: T
) : NBTNode {
    override val expanded get() = false
    override val depth = parent.depth + 1
    override suspend fun getChildren() = emptySet<NBTNode>()
    override fun hashCode() = this.uid
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is ValueNode<*> && this.data == other.data
    }
}

class ByteNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: Byte
) : ValueNode<Byte>(parent, uid, name, data) {
    override fun getLayout() = LAYOUT_BYTE
}

class ShortNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: Short
) : ValueNode<Short>(parent, uid, name, data) {
    override fun getLayout() = LAYOUT_SHORT
}

class IntNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: Int
) : ValueNode<Int>(parent, uid, name, data) {
    override fun getLayout() = LAYOUT_INT
}

class LongNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: Long
) : ValueNode<Long>(parent, uid, name, data) {
    override fun getLayout() = LAYOUT_LONG
}

class FloatNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: Float
) : ValueNode<Float>(parent, uid, name, data) {
    override fun getLayout() = LAYOUT_FLOAT
}

class DoubleNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: Double
) : ValueNode<Double>(parent, uid, name, data) {
    override fun getLayout() = LAYOUT_DOUBLE
}

class StringNode(
    parent: RootNode,
    uid: Int,
    name: String,
    data: String
) : ValueNode<String>(parent, uid, name, data) {
    override fun getLayout() = LAYOUT_STRING
}