package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.content.Context
import android.view.ContextMenu.ContextMenuInfo
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode

abstract class NodeHolder<V : ViewBinding, T : NBTNode>(
    val binding: V
) : ViewHolder(binding.root), ContextMenuInfo {
    val context: Context get() = this.binding.root.context
    var node: T? = null
        protected set

    abstract fun bind(node: NBTNode)
    abstract fun rename(name: String)
}