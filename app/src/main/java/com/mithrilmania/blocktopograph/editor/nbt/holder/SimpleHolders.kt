package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mithrilmania.blocktopograph.databinding.TagDefaultLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagRootLayoutBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTAdapter
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode

class RootHolder(
    parent: ViewGroup
) : NodeHolder<TagRootLayoutBinding, NBTAdapter>(
    TagRootLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) {
    override fun bind(node: NBTNode) {
        this.node = node as? NBTAdapter
        this.binding.tagName.text = node.name
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }
}

class UnknownHolder(
    parent: ViewGroup
) : NodeHolder<TagDefaultLayoutBinding, NBTNode>(
    TagDefaultLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) {
    override fun bind(node: NBTNode) {
        this.node = node
        this.binding.tagName.text = node.name
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }
}