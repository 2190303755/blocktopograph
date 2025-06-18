package com.mithrilmania.blocktopograph.editor.nbt;

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder

class NBTEditorView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, style: Int) : super(context, attrs, style)

    private var holder: NodeHolder<*, *>? = null
    override fun getContextMenuInfo() = this.holder

    fun getNodeHolder(child: View): NodeHolder<*, *>? = if (this == child.parent)
        this.getChildViewHolder(child) as? NodeHolder<*, *>
    else null

    override fun showContextMenuForChild(child: View): Boolean {
        this.holder = this.getNodeHolder(child)
        return super.showContextMenuForChild(child)
    }
}
