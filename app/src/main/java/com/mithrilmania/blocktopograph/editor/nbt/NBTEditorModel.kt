package com.mithrilmania.blocktopograph.editor.nbt

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.nbt.EMPTY_COMPOUND
import com.mithrilmania.blocktopograph.nbt.NBTInput
import com.mithrilmania.blocktopograph.storage.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import java.lang.ref.WeakReference

class NBTEditorModel : ViewModel() {
    val modified: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: MutableLiveData<Boolean> = MutableLiveData(false)
    val stringify: MutableLiveData<Boolean> = MutableLiveData(false)
    val prettify: MutableLiveData<Boolean> = MutableLiveData(true)
    val version: MutableLiveData<Byte?> = MutableLiveData()
    val variant: MutableLiveData<NbtVariant> = MutableLiveData(NbtVariant.Bedrock)
    val compression: MutableLiveData<NbtCompression> = MutableLiveData(NbtCompression.None)
    val source: MutableLiveData<File?> = MutableLiveData()
    val tree: MutableLiveData<NBTTree> = MutableLiveData()
    val history: MutableLiveData<HistoryState> = MutableLiveData(HistoryState(false, false))
    private val undo: ArrayDeque<Operation<*>> = ArrayDeque()
    private val redo: ArrayDeque<Operation<*>> = ArrayDeque()
    var holder: WeakReference<NodeHolder<*, *>>? = null
    var name: String = ""
        set(value) {
            field = value
            this.holder?.get()?.onRename(value)
        }

    suspend fun accept(input: NBTInput) {
        name = input.name
        withContext(Dispatchers.Main) {
            undo.clear()
            redo.clear()
            loading.value = false
            stringify.value = input.string
            variant.value = input.variant
            compression.value = input.compression
            version.value = input.version
            tree.value = NBTTree(this@NBTEditorModel, input.data)
        }
    }

    fun reset() {
        name = ""
        undo.clear()
        redo.clear()
        source.value = null
        loading.value = false
        stringify.value = true
        variant.value = NbtVariant.Bedrock
        compression.value = NbtCompression.None
        version.value = null
        tree.value = NBTTree(this, EMPTY_COMPOUND)
    }

    operator fun plusAssign(operation: Operation<*>) {
        this.redo.clear()
        this.undo.addLast(operation)
        this.updateHistory()
    }

    fun undo() {
        val queue = this.undo
        if (queue.isEmpty()) return
        val action = queue.removeLast()
        this.redo.addLast(action)
        action.undo()
        this.updateHistory()
    }

    fun redo() {
        val queue = this.redo
        if (queue.isEmpty()) return
        val action = queue.removeLast()
        this.undo.addLast(action)
        action.redo()
        this.updateHistory()
    }

    fun markDirty() {
        if (this.modified.value == true) return
        this.modified.value = true
    }

    data class HistoryState(val undo: Boolean, val redo: Boolean)

    fun updateHistory() {
        this.history.value = HistoryState(this.undo.isNotEmpty(), this.redo.isNotEmpty())
        this.markDirty()
    }
}