package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.mithrilmania.blocktopograph.databinding.TagCompoundLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagListLayoutBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTAdapter
import com.mithrilmania.blocktopograph.editor.nbt.NBTAdapter.Companion.reloadAsync
import com.mithrilmania.blocktopograph.editor.nbt.node.CollectionNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.applyIndent

abstract class CollectionHolder<T : ViewBinding>(
    val adapter: NBTAdapter,
    parent: ViewGroup,
    binding: (LayoutInflater, ViewGroup, Boolean) -> T,
) : NodeHolder<T>(
    parent.context,
    binding(LayoutInflater.from(parent.context), parent, false)
), View.OnClickListener {
    protected var node: CollectionNode<*, *>? = null

    init {
        this.binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val node = this.node ?: return
        node.expanded = !node.expanded
        this.adapter.reloadAsync()
    }
}

class CompoundHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : CollectionHolder<TagCompoundLayoutBinding>(adapter, parent, TagCompoundLayoutBinding::inflate) {
    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is CollectionNode<*, *>) {
            this.node = node
            this.binding.apply {
                applyIndent(node, this@CompoundHolder.context)
                tagName.text = node.name
            }
        }
    }
}

class ListHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : CollectionHolder<TagListLayoutBinding>(adapter, parent, TagListLayoutBinding::inflate) {
    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is CollectionNode<*, *>) {
            this.node = node
            this.binding.apply {
                applyIndent(node, this@ListHolder.context)
                tagName.text = node.name
            }
        }
    }
}