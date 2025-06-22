package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.nbt.EMPTY_COMPOUND
import com.mithrilmania.blocktopograph.nbt.readUnknownNBT
import com.mithrilmania.blocktopograph.storage.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import java.io.InputStream
import java.lang.ref.WeakReference

class NBTEditorModel : ViewModel(), ExporterFactory {
    val modified: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: MutableLiveData<Boolean> = MutableLiveData(false)
    val stringify: MutableLiveData<Boolean> = MutableLiveData(false)
    val prettify: MutableLiveData<Boolean> = MutableLiveData(true)
    val version: MutableLiveData<Byte?> = MutableLiveData()
    val variant: MutableLiveData<NbtVariant> = MutableLiveData(NbtVariant.Bedrock)
    val compression: MutableLiveData<NbtCompression> = MutableLiveData(NbtCompression.None)
    val source: MutableLiveData<File?> = MutableLiveData()
    val tree: MutableLiveData<NBTTree> = MutableLiveData(null)
    val history: MutableLiveData<HistoryState> = MutableLiveData(HistoryState(false, false))
    private val undo: ArrayDeque<Operation<*>> = ArrayDeque()
    private val redo: ArrayDeque<Operation<*>> = ArrayDeque()
    var holder: WeakReference<NodeHolder<*, *>>? = null
    var name: String = ""
        set(value) {
            field = value
            this.holder?.get()?.onRename(value)
        }

    fun readFileAsync(file: File, context: Context) {
        loading.value = true
        source.value = file
        val app = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val input = file.read(app, InputStream::readUnknownNBT) ?: return@launch
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
    }

    fun saveFileAsync(file: File, context: Context, factory: ExporterFactory = this) {
        source.value = file
        val app = context.applicationContext
        viewModelScope.launch(Dispatchers.Default) {
            val data = tree.value?.asTag() ?: return@launch
            withContext(Dispatchers.IO) {
                source.value?.save(app) { stream ->
                    factory.createExporter().write(stream, data, name)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(app, "Done", Toast.LENGTH_SHORT).show()
                modified.value = false
            }
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

    fun updateHistory() {
        this.history.value = HistoryState(this.undo.isNotEmpty(), this.redo.isNotEmpty())
        this.markDirty()
    }

    override fun createExporter() = if (this.stringify.value == false) NBTOptions(
        this.variant.value ?: NbtVariant.Bedrock,
        this.compression.value ?: NbtCompression.None,
        this.version.value
    ) else SNBTOptions(this.prettify.value == false)

    data class HistoryState(val undo: Boolean, val redo: Boolean)
}