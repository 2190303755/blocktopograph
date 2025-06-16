package com.mithrilmania.blocktopograph.editor.nbt;

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class NBTEditorView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, style: Int) : super(context, attrs, style)

    private var info: ContextMenuInfo? = null
    override fun getContextMenuInfo() = this.info

    override fun showContextMenuForChild(view: View): Boolean {
        this.info = ContextMenuInfo(this.getChildViewHolder(view))
        return super.showContextMenuForChild(view)
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        if (adapter !is NBTAdapter) throw IllegalArgumentException()
        super.setAdapter(adapter)
    }

    class ContextMenuInfo(val holder: ViewHolder) : ContextMenu.ContextMenuInfo
}
