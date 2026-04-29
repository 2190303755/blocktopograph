package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.PrimitiveTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.util.NBTStringifier
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts

private val UID: AtomicInteger = AtomicInteger()

interface RootLike {
    val depth: Int
}

sealed class NBTNode(
    val parent: RootLike,
    key: Any
) {
    val uid: Int = UID.getAndIncrement()
    var key: Any by mutableStateOf(key)
    val depth: Int = parent.depth + 1
    var showContextMenu: Boolean by mutableStateOf(false)
    abstract val type: Byte
    abstract var expanded: Boolean
    abstract val children: Collection<NBTNode>
    abstract fun toBinaryTag(): BinaryTag

    @Composable
    abstract fun Content(modifier: Modifier = Modifier)

    @Composable
    abstract fun ContextMenu(editor: NBTEditorModel)
}

sealed class RootNode(
    parent: RootLike,
    key: Any
) : NBTNode(parent, key), RootLike {
    override var expanded: Boolean by mutableStateOf(parent !is RootNode)
    abstract fun remove(key: Any): NBTNode?
    abstract fun insert(key: Any, node: NBTNode): Boolean
    abstract fun replace(key: Any, node: NBTNode): Boolean
}

sealed class ValueNode(
    parent: RootLike,
    key: Any
) : NBTNode(parent, key) {
    override var expanded: Boolean
        set(_) {}
        get() = false
    override val children: Collection<NBTNode> get() = emptyList()
    abstract override fun toBinaryTag(): PrimitiveTag

    @Composable
    override fun ContextMenu(editor: NBTEditorModel) {

    }
}

sealed class CollectionNode<N : NBTNode, T : BinaryTag>(
    parent: RootLike,
    key: Any,
    tag: T?
) : RootNode(parent, key) {
    override val children: MutableList<N> = if (tag === null) {
        mutableListOf()
    } else {
        this.buildNodes(tag)
    }

    abstract override fun toBinaryTag(): T
    abstract fun cast(node: NBTNode): N?
    abstract fun buildNodes(tag: T): MutableList<N>
    override fun insert(key: Any, node: NBTNode): Boolean {
        if (key is Number) {
            val value = this.cast(node)
            if (value !== null) {
                this.children.add(key.toInt(), value)
                return true
            }
        }
        return false
    }

    override fun replace(key: Any, node: NBTNode): Boolean {
        if (key is Number) {
            val index = key.toInt()
            if (index in 0 until this.children.size) {
                val value = this.cast(node)
                if (value !== null) {
                    this.children[index] = value
                    return true
                }
            }
        }
        return false
    }

    override fun remove(key: Any): NBTNode? {
        if (key is Number) {
            val index = key.toInt()
            if (index in 0 until children.size) {
                return this.children.removeAt(index)
            }
        }
        return null
    }

    fun swap(a: Int, b: Int): Boolean {
        if (a < 0 || b < 0) return false
        val nodes = this.children
        if (maxOf(a, b) >= nodes.size) return false
        val temp = nodes[a]
        nodes[a] = nodes[b]
        nodes[a].key = a
        nodes[b] = temp
        temp.key = b
        return true
    }
}

fun NBTNode.visit(action: (NBTNode) -> Unit) {
    action(this)
    if (this.expanded) {
        this.children.forEach {
            it.visit(action)
        }
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
fun BinaryTag.buildNode(
    parent: RootLike,
    key: Any
): NBTNode = when (this) {
    is CompoundTag -> MapNode(parent, key, this)
    is ByteArrayTag -> ByteArrayNode(parent, key, this)
    is IntArrayTag -> IntArrayNode(parent, key, this)
    is LongArrayTag -> LongArrayNode(parent, key, this)
    is ListTag -> ListNode(parent, key, this)
    is ByteTag -> ByteNode(parent, key, this.value)
    is ShortTag -> ShortNode(parent, key, this.value)
    is IntTag -> IntNode(parent, key, this.value)
    is LongTag -> LongNode(parent, key, this.value)
    is FloatTag -> FloatNode(parent, key, this.value)
    is DoubleTag -> DoubleNode(parent, key, this.value)
    is StringTag -> StringNode(parent, key, this.value)
    else -> throw IllegalArgumentException()
}

fun NBTNode.stringify(heterogeneous: Boolean = true): String {
    val stringifier: NBTStringifier
    if (this.parent is ListNode) {
        stringifier = NBTStringifier(heterogeneous = heterogeneous)
    } else {
        val name = this.key.toString()
        stringifier = NBTStringifier(
            heterogeneous = heterogeneous,
            builder = StringBuilder(name.length + 32)
                .appendSafeLiteral(name)
                .append(':')
                .append(' ')
        )
    }
    this.toBinaryTag().accept(stringifier)
    return stringifier.toString()
}

/*
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.contextMenuNode(
    node: NBTNode,
    editor: NBTEditorModel,
    onRename: (NBTNode, MapNode, String, String) -> Unit = { _, _, _, _ -> },
    onReplace: (NBTNode, MapNode, NBTNode) -> Unit = { _, _, _ -> },
    onDelete: (NBTNode, RootNode, Any) -> Unit = { _, _, _ -> },
    onMove: (NBTNode, ListNode, Int, Int) -> Unit = { _, _, _, _ -> },
    onCopy: (NBTNode, String) -> Unit = { _, _ -> },
    onInsert: (RootNode, NBTNode) -> Unit = { _, _ -> }
): Modifier {
    var showMenu by remember { mutableStateOf(false) }

    return this
        .pointerInput(node) {
            awaitEachChild {
                when (it.type) {
                    PointerEventType.LongPress -> {
                        showMenu = true
                    }
                }
                false
            }
        }
        .then(
            Box {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = {
                            onCopy(node, node.stringify())
                            showMenu = false
                        }
                    )

                    val parentAsMap = node.getParentAsMapNode()
                    val parentAsList = node.getParentAsListNode()

                    if (parentAsMap != null) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                onRename(node, parentAsMap, node.key.toString(), "")
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Replace") },
                            onClick = {
                                onReplace(node, parentAsMap, node)
                                showMenu = false
                            }
                        )
                    }

                    if (parentAsList != null) {
                        val index = parentAsList.children.indexOf(node)
                        if (index > 0) {
                            DropdownMenuItem(
                                text = { Text("Move Up") },
                                onClick = {
                                    onMove(node, parentAsList, index, index - 1)
                                    showMenu = false
                                }
                            )
                        }
                        if (index + 1 < parentAsList.children.size) {
                            DropdownMenuItem(
                                text = { Text("Move Down") },
                                onClick = {
                                    onMove(node, parentAsList, index, index + 1)
                                    showMenu = false
                                }
                            )
                        }
                    }

                    if (node is MapNode || node is ListNode) {
                        DropdownMenuItem(
                            text = { Text("Insert") },
                            onClick = {
                                onInsert(node, node)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        )
}


.contextMenuNode(
node = node,
editor = viewModel,
onCopy = { n, text ->
    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copy", text)
    clipboard.setPrimaryClip(clip)
},
onDelete = { n, parent, key ->
    viewModel.performOperation(
        object : Operation {
            private val removedNode = n
            private val parentRef = parent
            private val keyRef = key
            override fun redo() {
                parentRef.remove(keyRef)
                viewModel.expandNode(parentRef as? com.mithrilmania.blocktopograph.editor.nbt.NBTNode
                    ?: return)
            }
            override fun undo() {
                parentRef.insert(keyRef, removedNode)
            }
        }
    )
},
onMove = { n, listNode, oldIndex, newIndex ->
    viewModel.performOperation(
        object : Operation {
            private val targetNode = n
            private val parentList = listNode
            override fun redo() {
            }
            override fun undo() {
            }
        }
    )
},
onRename = { n, mapNode, oldName, newName ->
    viewModel.performOperation(
        object : Operation {
            private val targetNode = n
            private val parentMap = mapNode
            override fun redo() {
                targetNode.key = newName
            }
            override fun undo() {
                targetNode.key = oldName
            }
        }
    )
},
onReplace = { oldNode, parent, newNode ->
    viewModel.performOperation(
        object : Operation {
            private val old = oldNode
            private val parentMap = parent
            private val neo = newNode
            override fun redo() {
                parentMap.replace(old.key, neo)
            }
            override fun undo() {
                parentMap.replace(neo.key, old)
            }
        }
    )
},
onInsert = { parent, node ->
    this@NeoNBTEditorActivity.upcoming()
}
)*/