package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mithrilmania.blocktopograph.databinding.TagDefaultLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagRootLayoutBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTTree
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode

class RootHolder(
    parent: ViewGroup
) : NodeHolder<TagRootLayoutBinding, NBTTree>(
    TagRootLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) {
    override fun bind(node: NBTNode) {
        this.node = node as? NBTTree
        this.binding.root.text = node.name
    }

    override fun onRename(name: String) {
        this.binding.root.text = name
    }
}

class UnknownHolder(
    parent: ViewGroup
) : NodeHolder<TagDefaultLayoutBinding, NBTNode>(
    TagDefaultLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) {
    override fun bind(node: NBTNode) {
        this.node = node
        this.binding.root.text = node.name
    }

    override fun onRename(name: String) {
        this.binding.root.text = name
    }
}