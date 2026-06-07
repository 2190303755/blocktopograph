package com.mithrilmania.blocktopograph.editor.nbt

import com.mithrilmania.blocktopograph.editor.nbt.node.CollectionNode
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.editor.nbt.node.visit

interface Operation {
    fun redo(editor: NBTEditorModel)
    fun undo(editor: NBTEditorModel)
}

class Insert(
    val parent: RootNode,
    val child: NBTNode
) : Operation {
    override fun redo(editor: NBTEditorModel) {
        editor.adjustChild(this.parent) {
            this.parent.insert(this.child.key, this.child)
        }
    }

    override fun undo(editor: NBTEditorModel) {
        if (editor.focused === this.child) {
            editor.focused = null
        }
        editor.adjustChild(this.parent) {
            this.parent.remove(this.child.key)
        }
    }
}

class Delete(
    val parent: RootNode,
    val child: NBTNode
) : Operation {
    override fun redo(editor: NBTEditorModel) {
        if (editor.focused === this.child) {
            editor.focused = null
        }
        editor.adjustChild(this.parent) {
            this.parent.remove(this.child.key)
        }
    }

    override fun undo(editor: NBTEditorModel) {
        editor.adjustChild(this.parent) {
            this.parent.insert(this.child.key, this.child)
        }
    }
}

class Swap(
    val parent: CollectionNode<*, *>,
    val a: Int,
    val b: Int
) : Operation {
    override fun redo(editor: NBTEditorModel) {
        editor.adjustChild(this.parent) {
            this.parent.swap(this.a, this.b)
        }
    }

    override fun undo(editor: NBTEditorModel) {
        editor.adjustChild(this.parent) {
            this.parent.swap(this.b, this.a)
        }
    }
}

class Replace(
    val parent: RootNode?,
    val old: NBTNode,
    val neo: NBTNode
) : Operation {
    override fun redo(editor: NBTEditorModel) {
        val parent = this.parent
        if (parent === null) {
            val nodes = mutableListOf<NBTNode>()
            this.neo.visit(nodes::add)
            editor.nodes.clear()
            editor.nodes.addAll(nodes)
        } else {
            editor.adjustChild(parent) {
                this.parent.remove(this.old.key)
                this.parent.insert(this.neo.key, this.neo)
            }
        }
        if (editor.focused === this.old) {
            editor.focused = this.neo
        }
    }

    override fun undo(editor: NBTEditorModel) {
        val parent = this.parent
        if (parent === null) {
            val nodes = mutableListOf<NBTNode>()
            this.old.visit(nodes::add)
            editor.nodes.clear()
            editor.nodes.addAll(nodes)
        } else {
            editor.adjustChild(parent) {
                this.parent.remove(this.neo.key)
                this.parent.insert(this.old.key, this.old)
            }
        }
        if (editor.focused === this.neo) {
            editor.focused = this.old
        }
    }
}

class Rename(
    val parent: MapNode?,
    val old: String,
    val neo: String
) : Operation {
    override fun redo(editor: NBTEditorModel) {
        val parent = this.parent
        if (parent === null) {
            editor.nodes.firstOrNull()?.key = this.neo
        } else {
            editor.adjustChild(this.parent) {
                this.parent.nodes.remove(this.old)?.let {
                    it.key = this.neo
                    this.parent.nodes[this.neo] = it
                }
            }
        }
    }

    override fun undo(editor: NBTEditorModel) {
        val parent = this.parent
        if (parent === null) {
            editor.nodes.firstOrNull()?.key = this.old
        } else {
            editor.adjustChild(this.parent) {
                this.parent.nodes.remove(this.neo)?.let {
                    it.key = this.old
                    this.parent.nodes[this.old] = it
                }
            }
        }
    }
}