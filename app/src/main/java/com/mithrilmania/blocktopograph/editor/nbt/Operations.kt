package com.mithrilmania.blocktopograph.editor.nbt

import com.mithrilmania.blocktopograph.editor.nbt.node.ListNode
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode

sealed class Operation<T : NBTNode>(val target: T) {
    abstract fun undo()
    abstract fun redo()
}

class Delete<K>(
    target: NBTNode,
    val parent: RootNode<K>,
    val key: K
) : Operation<NBTNode>(target) {
    override fun undo() {
        this.parent.tree.reloadAsync()
        this.parent.let {
            it.insert(this.key, this.target)
            if (it.expanded) {
                it.tree.reloadAsync()
            }
        }
    }

    override fun redo() {
        this.parent.let {
            it.remove(this.key)
            if (it.expanded) {
                it.tree.reloadAsync()
            }
        }
    }
}

class Insert<K>(target: NBTNode, val parent: RootNode<K>, val key: K) : Operation<NBTNode>(target) {
    override fun undo() {
        this.parent.let {
            it.remove(this.key)
            if (it.expanded) {
                it.tree.reloadAsync()
            }
        }
    }

    override fun redo() {
        this.parent.let {
            if (it.insert(this.key, this.target) && it.expanded) {
                it.tree.reloadAsync()
            }
        }
    }
}

class Replace(target: NBTNode, val parent: MapNode, val neo: NBTNode) : Operation<NBTNode>(target) {
    override fun undo() {
        this.parent.let {
            it.put(this.neo.name, this.target)
            if (it.expanded) {
                it.tree.reloadAsync()
            }
        }
    }

    override fun redo() {
        this.parent.let {
            it.put(this.target.name, this.neo)
            if (it.expanded) {
                it.tree.reloadAsync()
            }
        }
    }
}

class Move(
    target: NBTNode,
    val parent: ListNode,
    val old: Int,
    val neo: Int
) : Operation<NBTNode>(target) {
    override fun undo() {
        if (this.parent.swap(this.old, this.neo)) {
            this.parent.notifyMovedChildren()
        }
    }

    override fun redo() {
        if (this.parent.swap(this.old, this.neo)) {
            this.parent.notifyMovedChildren()
        }
    }
}

class Rename(
    target: NBTNode,
    val parent: MapNode,
    val old: String,
    val neo: String
) : Operation<NBTNode>(target) {
    override fun undo() {
        this.target.let {
            this.parent.remove(this.neo)
            it.name = this.old
            this.parent.put(this.old, it)
        }
    }

    override fun redo() {
        this.target.let {
            this.parent.remove(this.old)
            it.name = this.neo
            this.parent.put(this.neo, it)
        }
    }
}

class Relabel(target: NBTNode, val old: String, val neo: String) : Operation<NBTNode>(target) {
    override fun undo() {
        this.target.name = this.old
    }

    override fun redo() {
        this.target.name = this.neo
    }
}