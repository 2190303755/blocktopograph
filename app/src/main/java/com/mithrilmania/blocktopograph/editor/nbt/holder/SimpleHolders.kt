package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mithrilmania.blocktopograph.databinding.TagDefaultLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagRootLayoutBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTAdapter
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.applyIndent

class RootHolder(
    parent: ViewGroup
) : NodeHolder<TagRootLayoutBinding>(
    parent.context,
    TagRootLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) {
    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        this.binding.apply {
            applyIndent(node, this@RootHolder.context)
            tagName.text = node.name
        }
    }
}

class UnknownHolder(
    parent: ViewGroup
) : NodeHolder<TagDefaultLayoutBinding>(
    parent.context,
    TagDefaultLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) {
    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        this.binding.apply {
            applyIndent(node, this@UnknownHolder.context)
            tagName.text = node.name
        }
    }
}