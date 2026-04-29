package com.mithrilmania.blocktopograph.editor.nbt

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootLike
import com.mithrilmania.blocktopograph.editor.nbt.node.buildNode
import com.mithrilmania.blocktopograph.editor.nbt.node.visit
import com.mithrilmania.blocktopograph.nbt.BinaryTag

fun NBTNode.collectVisibleChildren(): List<NBTNode> {
    val children = mutableListOf<NBTNode>()
    this.children.forEach {
        it.visit(children::add)
    }
    return children
}

fun NBTNode.countOfVisibleNodes(): Int {
    var count = 0
    this.visit { ++count }
    return count
}

open class NBTTreeModel : ViewModel(), RootLike {
    override val depth: Int get() = 0
    val nodes = mutableStateListOf<NBTNode>()

    fun expandNode(node: NBTNode) {
        if (!node.expanded) {
            val index = this.nodes.indexOf(node)
            if (index < 0) return
            this.nodes.addAll(index + 1, node.collectVisibleChildren())
        }
    }

    fun collapsesNode(node: NBTNode) {
        if (node.expanded) {
            val index = this.nodes.indexOf(node)
            if (index < 0 || index + 1 >= this.nodes.size) return
            val offset = node.countOfVisibleNodes()
            if (offset > 1) {
                this.nodes.removeRange(index + 1, index + offset)
            }
        }
    }

    fun adjustChild(
        parent: NBTNode,
        action: () -> Unit
    ) {
        if (parent.expanded) {
            val index = this.nodes.indexOf(parent)
            if (index >= 0) {
                val offset = parent.countOfVisibleNodes()
                if (offset > 1) {
                    this.nodes.removeRange(index + 1, index + offset)
                }
                action()
                this.nodes.addAll(index + 1, parent.collectVisibleChildren())
                return
            }
        }
        action()
    }

    fun flattenTag(tag: BinaryTag, name: String = ""): List<NBTNode> {
        val nodes = mutableListOf<NBTNode>()
        val root = tag.buildNode(
            this,
            name
        )
        root.visit(nodes::add)
        return nodes
    }
}