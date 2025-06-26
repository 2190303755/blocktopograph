package com.mithrilmania.blocktopograph.editor.nbt.node

import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.io.runSilent
import com.mithrilmania.blocktopograph.nbt.toBinaryTag

fun NBTNode?.toByteNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = when (this) {
    is StringNode -> ByteNode(parent, uid, name, this.value.toByteOrNull() ?: 0)
    is NumericNode<*> -> ByteNode(parent, uid, name, this.value.toByte())
    else -> ByteNode(parent, uid, name, 0)
}

fun NBTNode?.toShortNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = when (this) {
    is StringNode -> ShortNode(parent, uid, name, this.value.toShortOrNull() ?: 0)
    is NumericNode<*> -> ShortNode(parent, uid, name, this.value.toShort())
    else -> ShortNode(parent, uid, name, 0)
}

fun NBTNode?.toIntNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = when (this) {
    is StringNode -> IntNode(parent, uid, name, this.value.toIntOrNull() ?: 0)
    is NumericNode<*> -> IntNode(parent, uid, name, this.value.toInt())
    else -> IntNode(parent, uid, name, 0)
}

fun NBTNode?.toLongNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = when (this) {
    is StringNode -> LongNode(parent, uid, name, this.value.toLongOrNull() ?: 0)
    is NumericNode<*> -> LongNode(parent, uid, name, this.value.toLong())
    else -> LongNode(parent, uid, name, 0)
}

fun NBTNode?.toFloatNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = when (this) {
    is StringNode -> FloatNode(parent, uid, name, this.value.toFloatOrNull() ?: 0.0F)
    is NumericNode<*> -> FloatNode(parent, uid, name, this.value.toFloat())
    else -> FloatNode(parent, uid, name, 0.0F)
}

fun NBTNode?.toDoubleNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = when (this) {
    is StringNode -> DoubleNode(parent, uid, name, this.value.toDoubleOrNull() ?: 0.0)
    is NumericNode<*> -> DoubleNode(parent, uid, name, this.value.toDouble())
    else -> DoubleNode(parent, uid, name, 0.0)
}

fun NBTNode?.toStringNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = StringNode(parent, uid, name, if (this is ValueNode<*>) this.value.toString() else "")

fun NBTNode?.toCompoundNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = CompoundNode(parent, uid, name, if (this is MapNode) this.asTag() else null)

fun NBTNode?.toTagListNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
) = when (this) {
    null -> TagListNode(parent, uid, name, null)
    is ListNode -> {
        val tag = ListTag()
        runSilent {
            for (node in this.getChildren()) {
                tag += node.asTag()
            }
        }
        TagListNode(parent, uid, name, tag)
    }

    else -> {
        val tag = ListTag()
        tag += this.asTag()
        TagListNode(parent, uid, name, tag)
    }
}

fun NBTNode?.toByteArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
): ByteArrayNode {
    when (this) {
        is ListNode -> {
            val list = ArrayList<ByteTag>()
            for (node in this.getChildren()) {
                list += when (node) {
                    is StringNode -> node.value.toByteOrNull()?.toBinaryTag()
                        ?: return ByteArrayNode(parent, uid, name, null)

                    is NumericNode<*> -> node.value.toByte().toBinaryTag()
                    else -> return ByteArrayNode(parent, uid, name, null)
                }
            }
            return ByteArrayNode(parent, uid, name, list)
        }

        is StringNode -> {
            val value = this.value.toByteOrNull()?.toBinaryTag()
                ?: return ByteArrayNode(parent, uid, name, null)
            return ByteArrayNode(parent, uid, name, arrayListOf(value))
        }

        is NumericNode<*> -> return ByteArrayNode(
            parent, uid, name, arrayListOf(
                this.value.toByte().toBinaryTag()
            )
        )

        else -> return ByteArrayNode(parent, uid, name, null)
    }
}


fun NBTNode?.toIntArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
): IntArrayNode {
    when (this) {
        is ListNode -> {
            val list = ArrayList<IntTag>()
            for (node in this.getChildren()) {
                list += when (node) {
                    is StringNode -> node.value.toIntOrNull()?.toBinaryTag()
                        ?: return IntArrayNode(parent, uid, name, null)

                    is NumericNode<*> -> node.value.toInt().toBinaryTag()
                    else -> return IntArrayNode(parent, uid, name, null)
                }
            }
            return IntArrayNode(parent, uid, name, list)
        }

        is StringNode -> {
            val value = this.value.toIntOrNull()?.toBinaryTag()
                ?: return IntArrayNode(parent, uid, name, null)
            return IntArrayNode(parent, uid, name, arrayListOf(value))
        }

        is NumericNode<*> -> return IntArrayNode(
            parent, uid, name, arrayListOf(
                this.value.toInt().toBinaryTag()
            )
        )

        else -> return IntArrayNode(parent, uid, name, null)
    }
}


fun NBTNode?.toLongArrayNode(
    parent: RootNode<*>,
    uid: Int,
    name: String
): LongArrayNode {
    when (this) {
        is ListNode -> {
            val list = ArrayList<LongTag>()
            for (node in this.getChildren()) {
                list += when (node) {
                    is StringNode -> node.value.toLongOrNull()?.toBinaryTag()
                        ?: return LongArrayNode(parent, uid, name, null)

                    is NumericNode<*> -> node.value.toLong().toBinaryTag()
                    else -> return LongArrayNode(parent, uid, name, null)
                }
            }
            return LongArrayNode(parent, uid, name, list)
        }

        is StringNode -> {
            val value = this.value.toLongOrNull()?.toBinaryTag()
                ?: return LongArrayNode(parent, uid, name, null)
            return LongArrayNode(parent, uid, name, arrayListOf(value))
        }

        is NumericNode<*> -> return LongArrayNode(
            parent, uid, name, arrayListOf(
                this.value.toLong().toBinaryTag()
            )
        )

        else -> return LongArrayNode(parent, uid, name, null)
    }
}
