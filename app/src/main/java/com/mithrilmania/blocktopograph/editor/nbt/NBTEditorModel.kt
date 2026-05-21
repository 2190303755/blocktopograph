package com.mithrilmania.blocktopograph.editor.nbt

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootLike
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.editor.nbt.node.buildNode
import com.mithrilmania.blocktopograph.editor.nbt.node.visit
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
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

fun NBTNode.collectVisibleChildren(): List<NBTNode> {
    val children = mutableListOf<NBTNode>()
    this.children.forEach {
        it.visit(children::add)
    }
    return children
}

fun NBTNode.countOfVisibleNodes(): Int {
    var count = 0
    this.visit { ++count }
    return count
}

class NBTEditorModel(app: Application) : AndroidViewModel(app), NBTExportConfig, RootLike {
    override val depth: Int get() = 0
    var navigation: Pair<NBTSource, NBTImportConfig>? = null
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
    val nodes = mutableStateListOf<NBTNode>()
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
    suspend fun readFromFile(source: NBTSource, importer: NBTImportConfig) {
        val context = this.application
        val result = try {
            withContext(Dispatchers.IO) {
                source.readNBT(context, importer)
            } ?: return
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to read", Toast.LENGTH_SHORT).show()
            Log.e("NBTEditor", "Failed to read $source", e)
            return
        }
        flattening = true
        val flattened = withContext(Dispatchers.Default) {
            flattenTag(result.tag, result.name)
        }
        nodes.clear()
        nodes.addAll(flattened)
        this.source = source
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
    suspend fun saveToFile(source: NBTSource) {
        val context = this.application
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

    fun expandNode(node: NBTNode) {
        if (!node.expanded) {
            val index = this.nodes.indexOf(node)
            if (index < 0) return
            this.nodes.addAll(index + 1, node.collectVisibleChildren())
        }
    }

    fun collapsesNode(node: NBTNode) {
        if (node.expanded) {
            val index = this.nodes.indexOf(node)
            if (index < 0 || index + 1 >= this.nodes.size) return
            val offset = node.countOfVisibleNodes()
            if (offset > 1) {
                this.nodes.removeRange(index + 1, index + offset)
            }
        }
    }

    fun adjustChild(
        parent: NBTNode,
        action: () -> Unit
    ) {
        if (parent.expanded) {
            val index = this.nodes.indexOf(parent)
            if (index >= 0) {
                val offset = parent.countOfVisibleNodes()
                if (offset > 1) {
                    this.nodes.removeRange(index + 1, index + offset)
                }
                action()
                this.nodes.addAll(index + 1, parent.collectVisibleChildren())
                return
            }
        }
        action()
    }

    fun flattenTag(tag: BinaryTag, name: String = ""): List<NBTNode> {
        val nodes = mutableListOf<NBTNode>()
        val root = tag.buildNode(
            this,
            name
        )
        root.visit(nodes::add)
        return nodes
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