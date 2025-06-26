package com.mithrilmania.blocktopograph.editor.nbt

import android.util.SparseArray
import android.view.ViewGroup
import androidx.core.util.size
import androidx.core.view.updatePadding
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.holder.ByteHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.CompoundHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.DoubleHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.FloatHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.IntHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.ListHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.LongHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.RootHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.ShortHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.StringHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.UnknownHolder
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.collect
import com.mithrilmania.blocktopograph.editor.nbt.node.registerTo
import com.mithrilmania.blocktopograph.editor.nbt.node.visit
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.nbt.TAG_DOUBLE
import com.mithrilmania.blocktopograph.nbt.TAG_FLOAT
import com.mithrilmania.blocktopograph.nbt.TAG_INT
import com.mithrilmania.blocktopograph.nbt.TAG_INT_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_LIST
import com.mithrilmania.blocktopograph.nbt.TAG_LONG
import com.mithrilmania.blocktopograph.nbt.TAG_LONG_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_ROOT
import com.mithrilmania.blocktopograph.nbt.TAG_SHORT
import com.mithrilmania.blocktopograph.nbt.TAG_STRING
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

class NBTTree(
    val model: NBTEditorModel,
    data: CompoundTag?
) : ListAdapter<NBTNode, NodeHolder<*, *>>(object : DiffUtil.ItemCallback<NBTNode>() {
    override fun areItemsTheSame(old: NBTNode, neo: NBTNode) =
        old.uid == neo.uid

    override fun areContentsTheSame(old: NBTNode, neo: NBTNode) =
        old.isSame(neo)
}), MapNode {
    override val uid get() = 0
    override val depth get() = 0
    override val parent get() = null
    override val expanded get() = true
    override val tree get() = this
    override var name by model::name
    override var holder by model::holder
    override val type get() = TAG_ROOT
    private val nodes = SparseArray<NBTNode>()
    private val nodeId = AtomicInteger()
    private val taskId = AtomicInteger()
    val data by lazy { data.registerTo(this) }

    init {
        this.nodes[0] = this
    }

    override fun getChildren() = this.data.values
    override fun asTag() = this.data.collect()
    override fun remove(key: String) = this.data.remove(key)
    override fun containsKey(key: String) = this.data.containsKey(key)
    override fun put(key: String, node: NBTNode) = this.data.put(key, node)
    override fun insert(key: String, node: NBTNode): Boolean {
        if (this.data.containsKey(key)) return false
        this.data.put(key, node)
        return true
    }

    fun <T : NBTNode> register(key: String, factory: (Int, String) -> T): T {
        val uid = this.nodeId.incrementAndGet()
        val value = factory(uid, key)
        this.nodes[uid] = value
        return value
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = when (type) {
        TAG_BYTE -> ByteHolder(this, parent)
        TAG_SHORT -> ShortHolder(this, parent)
        TAG_INT -> IntHolder(this, parent)
        TAG_LONG -> LongHolder(this, parent)
        TAG_FLOAT -> FloatHolder(this, parent)
        TAG_DOUBLE -> DoubleHolder(this, parent)
        TAG_STRING -> StringHolder(this, parent)
        TAG_COMPOUND -> CompoundHolder(this, parent)
        TAG_LIST,
        TAG_BYTE_ARRAY,
        TAG_INT_ARRAY,
        TAG_LONG_ARRAY -> ListHolder(this, parent)

        TAG_ROOT -> RootHolder(parent)
        else -> UnknownHolder(parent)
    }

    override fun onBindViewHolder(
        holder: NodeHolder<*, *>,
        position: Int
    ) {
        this.getItem(position).let {
            holder.binding.root.apply {
                updatePadding(
                    left = holder.context.resources.getDimensionPixelSize(
                        R.dimen.large_content_padding
                    ) * it.depth
                )
                isLongClickable = true
            }
            it.holder = WeakReference(holder)
            holder.bind(it)
        }
    }

    override fun getItemViewType(position: Int) =
        this.getItem(position).type

    override fun getItemId(position: Int) =
        this.getItem(position).uid.toLong()

    suspend fun reload(full: Boolean = false) {
        val model = this.model
        model.loading.value = true
        val list = ArrayList<NBTNode>(this.nodes.size + 16)
        val uid = this.taskId.incrementAndGet()
        withContext(Dispatchers.Default) {
            this@NBTTree.visit(full, list::add)
        }
        if (this.taskId.get() == uid) {
            this.submitList(list)
        }
        model.loading.value = false
    }

    fun reloadAsync(full: Boolean = false) {
        this.model.viewModelScope.launch {
            this@NBTTree.reload(full)
        }
    }

    fun notifyNodeChanged(node: NBTNode?) {
        val index = this.currentList.indexOf(node ?: return)
        if (index < 0) return
        this.notifyItemChanged(index)
    }
}