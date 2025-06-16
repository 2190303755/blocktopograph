package com.mithrilmania.blocktopograph.editor.nbt

import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_BYTE
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_BYTE_ARRAY
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_COMPOUND
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_DOUBLE
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_FLOAT
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_INT
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_INT_ARRAY
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_LIST
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_LONG
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_LONG_ARRAY
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_ROOT
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_SHORT
import com.mithrilmania.blocktopograph.editor.nbt.node.LAYOUT_STRING
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.editor.nbt.node.registerTo
import com.mithrilmania.blocktopograph.editor.nbt.node.visit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.benwoodworth.knbt.NbtCompound
import java.util.concurrent.atomic.AtomicInteger

class NBTAdapter(
    val model: NBTEditorModel,
    override val name: String,
    data: Deferred<NbtCompound>
) : ListAdapter<NBTNode, NodeHolder<*>>(Callback()), RootNode {
    override val uid get() = 0
    override val depth get() = 0
    override val expanded get() = true
    override fun getLayout() = LAYOUT_ROOT
    private val nodes = SparseArray<NBTNode>()
    private val generator = AtomicInteger()
    val lifecycleScope = CoroutineScope(Dispatchers.Main)

    val data by lazy {
        runBlocking {
            data.await().registerTo(this@NBTAdapter) { key, factory ->
                val uid = this@NBTAdapter.generator.incrementAndGet()
                val value = factory(uid, key)
                this@NBTAdapter.nodes[uid] = value
                value
            }
        }
    }

    init {
        this.nodes[0] = this
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = when (type) {
        LAYOUT_BYTE -> ByteHolder(this, parent)
        LAYOUT_SHORT -> ShortHolder(this, parent)
        LAYOUT_INT -> IntHolder(this, parent)
        LAYOUT_LONG -> LongHolder(this, parent)
        LAYOUT_FLOAT -> FloatHolder(this, parent)
        LAYOUT_DOUBLE -> DoubleHolder(this, parent)
        LAYOUT_STRING -> StringHolder(this, parent)
        LAYOUT_COMPOUND -> CompoundHolder(this, parent)
        LAYOUT_LIST,
        LAYOUT_BYTE_ARRAY,
        LAYOUT_INT_ARRAY,
        LAYOUT_LONG_ARRAY -> ListHolder(this, parent)

        LAYOUT_ROOT -> RootHolder(parent)
        else -> UnknownHolder(parent)
    }

    override fun onBindViewHolder(
        holder: NodeHolder<*>,
        position: Int
    ) {
        holder.bind(this, this.getItem(position))
    }

    override fun getItemViewType(position: Int) =
        this.getItem(position).getLayout()

    override fun getItemId(position: Int) =
        this.getItem(position).uid.toLong()

    override suspend fun getChildren() = this.data.values

    override fun onCurrentListChanged(previousList: List<NBTNode?>, currentList: List<NBTNode?>) {
        super.onCurrentListChanged(previousList, currentList)
    }

    suspend fun reload(onlyVisible: Boolean = true) {
        this.model.reloading.value = true
        val list = ArrayList<NBTNode>()
        withContext(Dispatchers.Default) {
            this@NBTAdapter.visit(onlyVisible, list::add)
        }
        this.submitList(list)
        this.model.reloading.value = false
    }

    override fun onAttachedToRecyclerView(view: RecyclerView) {
        super.onAttachedToRecyclerView(view)
        this.reloadAsync()
    }

    override fun onDetachedFromRecyclerView(view: RecyclerView) {
        super.onDetachedFromRecyclerView(view)
        this.lifecycleScope.cancel()
    }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = super.hashCode()

    class Callback : DiffUtil.ItemCallback<NBTNode>() {
        override fun areItemsTheSame(old: NBTNode, neo: NBTNode) =
            old.uid == neo.uid

        override fun areContentsTheSame(old: NBTNode, neo: NBTNode) =
            old == neo
    }

    companion object {
        fun NBTAdapter.reloadAsync(onlyVisible: Boolean = true) {
            this.lifecycleScope.launch {
                this@reloadAsync.reload(onlyVisible)
            }
        }
    }
}