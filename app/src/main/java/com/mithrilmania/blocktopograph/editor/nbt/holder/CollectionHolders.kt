package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.mithrilmania.blocktopograph.databinding.TagCompoundLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagListLayoutBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTTree
import com.mithrilmania.blocktopograph.editor.nbt.node.CollectionNode
import com.mithrilmania.blocktopograph.editor.nbt.node.CompoundNode
import com.mithrilmania.blocktopograph.editor.nbt.node.ListNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode

abstract class CollectionHolder<V : ViewBinding, T : RootNode<*>>(
    val tree: NBTTree,
    parent: ViewGroup,
    binding: (LayoutInflater, ViewGroup, Boolean) -> V,
) : NodeHolder<V, T>(
    binding(LayoutInflater.from(parent.context), parent, false)
), View.OnClickListener {
    init {
        this.binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val node = this.node as? CollectionNode<*, *> ?: return
        node.expanded = !node.expanded
        if (node.parent.expanded) {
            this.tree.reloadAsync()
        }
    }
}

class CompoundHolder(
    adapter: NBTTree,
    parent: ViewGroup
) : CollectionHolder<TagCompoundLayoutBinding, CompoundNode>(
    adapter,
    parent,
    TagCompoundLayoutBinding::inflate
) {
    override fun bind(node: NBTNode) {
        this.node = node as? CompoundNode ?: return
        this.binding.root.text = node.name
    }

    override fun onRename(name: String) {
        this.binding.root.text = name
    }
}

class ListHolder(
    adapter: NBTTree,
    parent: ViewGroup
) : CollectionHolder<TagListLayoutBinding, ListNode>(
    adapter,
    parent,
    TagListLayoutBinding::inflate
) {
    override fun bind(node: NBTNode) {
        this.node = node as? ListNode ?: return
        this.binding.tagName.text = node.name
    }

    override fun onRename(name: String) {
        this.binding.tagName.text = name
    }
}