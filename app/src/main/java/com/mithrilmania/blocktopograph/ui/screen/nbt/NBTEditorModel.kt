package com.mithrilmania.blocktopograph.ui.screen.nbt

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.BlankNBTEditor
import com.mithrilmania.blocktopograph.ui.NBTExportModel
import com.mithrilmania.blocktopograph.ui.NBTImportModel
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.MapNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.RootLike
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.RootNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.buildNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.visit
import com.mithrilmania.blocktopograph.util.todoI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

typealias ConfirmationRequest = () -> Unit

@JvmInline
value class InsertionRequest(val parent: RootNode)

@JvmInline
value class ReplacingRequest(val node: NBTNode)

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

class NBTEditorModel(
    context: Context,
    spec: AppNavKey.NBTEditor,
    modified: MutableState<Boolean> = mutableStateOf(false),
    confirmation: MutableState<ConfirmationRequest?> = mutableStateOf(null)
) : ViewModel(), NBTExportConfig, RootLike {
    @JvmField
    val appContext: Context = context.applicationContext
    override val canBeHeterogeneous: Boolean get() = true
    override val depth: Int get() = 0
    override fun makePath(child: String): String = ""
    var source: NBTSource? by mutableStateOf(null)
    var focused: NBTNode? by mutableStateOf(null)
    var modified: Boolean by modified
    var flattening: Boolean by mutableStateOf(false)
    override var stringify: Boolean by mutableStateOf(false)
    override var prettify: Boolean by mutableStateOf(true)
    override var compress: Boolean by mutableStateOf(false)
    override var heterogeneous: Boolean by mutableStateOf(false)
    override var littleEndian: Boolean by mutableStateOf(true)
    override var storageVersion: Int? by mutableStateOf(null)
    var confirmation: ConfirmationRequest? by confirmation
    var insertion: InsertionRequest? by mutableStateOf(null)
    var replacing: ReplacingRequest? by mutableStateOf(null)
    var renaming: RenamingRequest? by mutableStateOf(null)
    var exporter: NBTExportModel? by mutableStateOf(null)
    var importer: NBTImportModel? by mutableStateOf(null)
    var showProperties: Boolean by mutableStateOf(false)

    @JvmField
    val nodes = mutableStateListOf<NBTNode>()

    @JvmField
    val undo = mutableStateListOf<Operation>()

    @JvmField
    val redo = mutableStateListOf<Operation>()

    init {
        val initial = spec.initial
        if (initial === null) {
            if (spec !== BlankNBTEditor) {
                this.nodes.add(MapNode(this, ""))
            }
        } else if (spec.inferable) {
            source = initial.first
            viewModelScope.launch {
                readFromFile(initial.first, initial.second)
            }
        } else {
            source = initial.first
            importer = NBTImportModel(
                initial.first,
                initial.second.format,
                initial.second.header
            )
        }
    }

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

    @MainThread
    suspend fun readFromFile(source: NBTSource, config: NBTImportConfig) {
        val context = this.appContext
        val result = try {
            withContext(Dispatchers.IO) {
                source.readNBT(context, config)
            } ?: return
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to read".todoI18n(), Toast.LENGTH_SHORT).show()
            Log.e("NBTEditor", "Failed to read $source", e)
            return
        }
        flattening = true
        val flattened = withContext(Dispatchers.Default) {
            flattenTag(result.tag, result.name)
        }
        focused = null
        nodes.clear()
        nodes.addAll(flattened)
        this.source = source
        flattening = false
        if (result.stringified) {
            stringify = true
            storageVersion = null
        } else {
            stringify = false
            littleEndian = result.littleEndian
            compress = result.compress
            storageVersion = result.storageVersion
        }
        this.importer = null
        modified = false
    }

    @MainThread
    suspend fun saveToFile(source: NBTSource) {
        val context = this.appContext
        val exporter = this.exporter
        val root = this.nodes.firstOrNull() ?: return
        val tag = withContext(Dispatchers.Default) {
            root.toBinaryTag()
        }
        try {
            withContext(Dispatchers.IO) {
                source.saveNBT(
                    context,
                    exporter ?: this@NBTEditorModel,
                    root.key.toString(),
                    tag
                )
            }
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to save".todoI18n(), Toast.LENGTH_SHORT).show()
            Log.e("NBTEditor", "Failed to save $source", e)
            return
        }
        Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
        if (exporter !== null) {
            this.stringify = exporter.stringify
            this.prettify = exporter.prettify
            this.compress = exporter.compress
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
            compressed = this.compress,
            littleEndian = this.littleEndian,
            version = this.storageVersion
        )
    }
}