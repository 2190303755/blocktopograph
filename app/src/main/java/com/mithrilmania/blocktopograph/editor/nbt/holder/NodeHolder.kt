package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.content.Context
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTAdapter
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode

abstract class NodeHolder<T : ViewBinding>(
    val context: Context,
    val binding: T
) : ViewHolder(binding.root) {
    abstract fun bind(adapter: NBTAdapter, node: NBTNode)
}