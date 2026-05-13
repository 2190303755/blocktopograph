package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

enum class ConfirmationRequest {
    EXIT,
    NEW,
    OPEN,
    RELOAD
}

@JvmInline
value class InsertionRequest(val parent: RootNode)

@JvmInline
value class ReplacementRequest(val node: NBTNode)

@JvmInline
value class RenamingRequest(val node: NBTNode)

class NBTEditorModel : NBTTreeModel(), NBTExportConfig {
    var modified: Boolean by mutableStateOf(false)
    var flattening: Boolean by mutableStateOf(false)
    var confirmation: ConfirmationRequest? by mutableStateOf(null)
    var insertion: InsertionRequest? by mutableStateOf(null)
    var replacement: ReplacementRequest? by mutableStateOf(null)
    var renaming: RenamingRequest? by mutableStateOf(null)
    var toolbarVisible: Boolean by mutableStateOf(true)
    var source: NBTSource? by mutableStateOf(null)
    var exporter: NBTExportModel? by mutableStateOf(null)
    var importer: NBTImportModel? by mutableStateOf(null)
    override var stringify: Boolean by mutableStateOf(false)
    override var prettify: Boolean by mutableStateOf(true)
    override var compressed: Boolean by mutableStateOf(false)
    override var heterogeneous: Boolean by mutableStateOf(false)
    override var littleEndian: Boolean by mutableStateOf(true)
    override var storageVersion: UInt? by mutableStateOf(null)
    val undo: MutableList<Operation> = mutableStateListOf()
    val redo: MutableList<Operation> = mutableStateListOf()

    fun performUndo() {
        undo.removeLastOrNull()?.let {
            redo.add(it)
            it.undo(this)
            modified = true
        }
    }

    fun performRedo() {
        redo.removeLastOrNull()?.let {
            undo.add(it)
            it.redo(this)
            modified = true
        }
    }

    fun performOperation(operation: Operation) {
        operation.redo(this)
        undo.add(operation)
        redo.clear()
        modified = true
    }

    fun reset() {
        undo.clear()
        redo.clear()
        nodes.clear()
        confirmation = null
        source = null
        storageVersion = null
        modified = true // remind to save
        nodes.add(MapNode(this, ""))
    }

    @MainThread
    suspend fun readFromFile(importer: NBTImportModel, context: Context) {
        val result = try {
            withContext(Dispatchers.IO) {
                importer.source.readNBT(context, importer)
            } ?: return
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to read", Toast.LENGTH_SHORT).show()
            Log.e("NBTEditor", "Failed to read ${importer.source}", e)
            return
        }
        flattening = true
        val flattened = withContext(Dispatchers.Default) {
            flattenTag(result.tag, result.name)
        }
        nodes.clear()
        nodes.addAll(flattened)
        source = importer.source
        flattening = false
        if (result.stringified) {
            stringify = true
            littleEndian = true
            compressed = false
            storageVersion = null
        } else {
            stringify = false
            littleEndian = result.littleEndian
            compressed = result.compressed
            storageVersion = result.version
        }
        this.importer = null
        modified = false
    }

    @MainThread
    suspend fun saveToFile(source: NBTSource, context: Context) {
        val exporter = this.exporter
        val root = this.nodes.firstOrNull() ?: return
        val tag = withContext(Dispatchers.Default) {
            root.toBinaryTag()
        }
        try {
            withContext(Dispatchers.IO) {
                source.saveNBT(context, exporter ?: this@NBTEditorModel, root.key.toString(), tag)
            }
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
            Log.e("NBTEditor", "Failed to save $source", e)
            return
        }
        Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
        if (exporter !== null) {
            this.stringify = exporter.stringify
            this.prettify = exporter.prettify
            this.compressed = exporter.compressed
            this.littleEndian = exporter.littleEndian
            this.exporter = null
        }
        this.source = source
        this.modified = false
    }

    fun buildExporter(repick: Boolean = false) {
        this.exporter = NBTExportModel(
            source = this.source,
            repick = repick,
            stringify = this.stringify,
            prettify = this.prettify,
            heterogeneous = this.heterogeneous,
            compressed = this.compressed,
            littleEndian = this.littleEndian,
            version = this.storageVersion
        )
    }
}