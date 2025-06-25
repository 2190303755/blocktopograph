package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockOutputBuffer
import com.mithrilmania.blocktopograph.nbt.io.NBTOutput
import com.mithrilmania.blocktopograph.nbt.io.NBTOutputBuffer
import com.mithrilmania.blocktopograph.nbt.io.NBTOutputFactory
import com.mithrilmania.blocktopograph.nbt.io.NamedResult
import com.mithrilmania.blocktopograph.nbt.io.readUnknownNBT
import com.mithrilmania.blocktopograph.nbt.util.NBTStringifier
import com.mithrilmania.blocktopograph.storage.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream

class NBTEditorModel : ViewModel(), NBTOutputFactory {
    val modified: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: MutableLiveData<Boolean> = MutableLiveData(false)
    val version: MutableLiveData<UInt?> = MutableLiveData()
    var stringify = false
    var prettify = true
    var littleEndian = true
    var compressed = false
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
            val result = file.read(app, InputStream::readUnknownNBT) ?: return@launch
            withContext(Dispatchers.Main) {
                undo.clear()
                redo.clear()
                loading.value = false
                if (result is NamedResult) {
                    name = result.name
                    stringify = false
                    littleEndian = result.littleEndian
                    compressed = result.compressed
                    version.value = result.version
                } else {
                    name = ""
                    stringify = true
                    littleEndian = true
                    compressed = false
                    version.value = null
                }
                tree.value = NBTTree(this@NBTEditorModel, result.tag as? CompoundTag)
            }
        }
    }

    fun saveFileAsync(file: File, context: Context, factory: NBTOutputFactory = this) {
        source.value = file
        val app = context.applicationContext
        val model = this
        viewModelScope.launch(Dispatchers.Default) {
            val tag = model.tree.value?.asTag() ?: return@launch
            withContext(Dispatchers.IO) {
                model.source.value?.save(app) { stream ->
                    factory.createOutput(stream).save(model.name, tag)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(app, "Done", Toast.LENGTH_SHORT).show()
                model.modified.value = false
            }
        }
    }

    fun reset() {
        name = ""
        undo.clear()
        redo.clear()
        source.value = null
        loading.value = false
        stringify = true
        littleEndian = true
        compressed = false
        version.value = null
        tree.value = NBTTree(this, null)
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

    override fun createOutput(stream: OutputStream): NBTOutput {
        if (this.stringify) {
            val builder = NBTStringifier(indent = if (this.prettify) "    " else "")
            return NBTOutput { name, tag ->
                tag.accept(builder)
                stream.use {
                    it.write(builder.toString().toByteArray(Charsets.UTF_8))
                }
            }
        }
        if (this.littleEndian) {
            if (this.compressed) return NBTOutputBuffer(
                GZIPOutputStream(stream),
                ByteOrder.LITTLE_ENDIAN
            )
            val version = this.version.value
            if (version === null) return NBTOutputBuffer(stream, ByteOrder.LITTLE_ENDIAN)
            return BedrockOutputBuffer(stream, version)
        }
        return NBTOutputBuffer(
            if (this.compressed) GZIPOutputStream(stream) else stream,
            ByteOrder.BIG_ENDIAN
        )
    }

    @JvmRecord
    data class HistoryState(val undo: Boolean, val redo: Boolean)
}