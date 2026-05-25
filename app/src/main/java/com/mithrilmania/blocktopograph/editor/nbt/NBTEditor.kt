package com.mithrilmania.blocktopograph.editor.nbt

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.node.CollectionNode
import com.mithrilmania.blocktopograph.editor.nbt.node.ListNode
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootLike
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.editor.nbt.node.buildNode
import com.mithrilmania.blocktopograph.editor.nbt.node.stringify
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.util.getHomogenousTypeId
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.ui.component.AlertDialog
import com.mithrilmania.blocktopograph.ui.component.AnimatedBottomSheetDialog
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem
import com.mithrilmania.blocktopograph.ui.component.HiddenOrExpanded
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.PastableDialog
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.TopAppBar
import com.mithrilmania.blocktopograph.ui.component.cascadingMenu
import com.mithrilmania.blocktopograph.ui.component.clickableItem
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.collectText
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.upcoming
import kotlinx.coroutines.launch

fun NBTEditorModel.saveAsync() {
    val source = this.source
    if (source === null) {
        this.buildExporter()
    } else {
        this.viewModelScope.launch {
            saveToFile(source)
        }
    }
}

inline fun NBTEditorModel.requestOrExecute(
    request: ConfirmationRequest,
    executor: (ConfirmationRequest?) -> Unit
) {
    if (this.modified) {
        this.confirmation = request
    } else {
        executor(request)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NBTEditor(
    editor: NBTEditorModel = viewModel(),
    onExit: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) callback@{
        editor.importer = NBTImportModel(SAFFile(it ?: return@callback))
    }
    val onConfirm: (ConfirmationRequest?) -> Unit = onConfirm@{
        editor.confirmation = null
        when (it) {
            ConfirmationRequest.EXIT -> onExit()
            ConfirmationRequest.NEW -> editor.reset()
            ConfirmationRequest.RELOAD -> {
                editor.importer = NBTImportModel(
                    source = editor.source ?: return@onConfirm,
                    header = if (editor.storageVersion !== null || editor.littleEndian) {
                        HeaderPresence.UNCERTAIN
                    } else {
                        HeaderPresence.ABSENT
                    },
                    format = if (editor.stringify) {
                        NBTFormat.STRINGIFIED
                    } else if (editor.littleEndian) {
                        NBTFormat.LITTLE_ENDIAN
                    } else {
                        NBTFormat.BIG_ENDIAN
                    }
                )
            }

            ConfirmationRequest.OPEN -> picker.launch("*/*")
            else -> {}
        }
    }
    val context = LocalContext.current
    val creator = rememberLauncherForActivityResult(FileCreator) callback@{
        val file = SAFFile(it ?: return@callback)
        editor.viewModelScope.launch {
            editor.saveToFile(file)
            onConfirm(editor.confirmation)
        }
    }
    BackHandler(editor.modified) {
        editor.confirmation = ConfirmationRequest.EXIT
    }
    if (editor.confirmation !== null) {
        AlertDialog(
            onDismissRequest = {
                editor.confirmation = null
            },
            title = { Text("更改未保存") },
            neutralButton = {
                TextButton("继续编辑") {
                    editor.confirmation = null
                }
            },
            positiveButton = {
                TextButton("保存") {
                    val source = editor.source
                    if (source === null) {
                        editor.buildExporter()
                    } else {
                        editor.viewModelScope.launch {
                            editor.saveToFile(source)
                            onConfirm(editor.confirmation)
                        }
                    }
                }
            },
            negativeButton = {
                TextButton("不保存") {
                    onConfirm(editor.confirmation)
                }
            }
        ) {
            Text("如果不保存，您的更改将丢失")
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        editor.source?.resolveName(context)
                            ?: stringResource(R.string.nbt_editor)
                    )
                },
                subtitle = editor.storageVersion?.let { version ->
                    {
                        Text(
                            stringResource(
                                R.string.activity_nbt_editor_subtitle,
                                version.toLong()
                            )
                        )
                    }
                },
                actions = {
                    val resources = LocalResources.current
                    AppBarRow(maxItemCount = 2) {
                        cascadingMenu(
                            Icons.Filled.Inventory2,
                            resources.getString(R.string.action_file)
                        ) { showMenu ->
                            DropdownMenuItem(resources.getString(R.string.action_file_create)) {
                                editor.requestOrExecute(ConfirmationRequest.NEW, onConfirm)
                                showMenu.value = false
                            }
                            DropdownMenuItem(resources.getString(R.string.action_file_open)) {
                                editor.requestOrExecute(ConfirmationRequest.OPEN, onConfirm)
                                showMenu.value = false
                            }
                            DropdownMenuItem(
                                resources.getString(R.string.action_file_save),
                                editor.nodes.isNotEmpty()
                            ) {
                                editor.saveAsync()
                                showMenu.value = false
                            }
                            DropdownMenuItem(
                                resources.getString(R.string.action_file_save_as),
                                editor.nodes.isNotEmpty()
                            ) {
                                editor.buildExporter(true)
                                showMenu.value = false
                            }
                            DropdownMenuItem(
                                resources.getString(R.string.action_file_reload),
                                editor.source !== null
                            ) click@{
                                editor.requestOrExecute(ConfirmationRequest.RELOAD, onConfirm)
                                showMenu.value = false
                            }
                        }
                        clickableItem(
                            Icons.Filled.Info,
                            resources.getString(R.string.action_file_info),
                            false
                        ) {
                            context.upcoming()
                        }
                        clickableItem(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            resources.getString(R.string.action_quit)
                        ) {
                            editor.requestOrExecute(ConfirmationRequest.EXIT, onConfirm)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = editor.flattening,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(1.0F)
                    .padding(top = padding.calculateTopPadding())
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            HorizontalFloatingToolbar(
                expanded = true,
                modifier =
                    Modifier
                        .padding(padding)
                        .align(Alignment.BottomCenter)
                        .offset(y = -ScreenOffset)
                        .zIndex(1.0F),
                leadingContent = {
                    TooltipBox("undo") { tooltip ->
                        IconButton(
                            Icons.AutoMirrored.Filled.Undo,
                            tooltip,
                            editor.undo.isNotEmpty()
                        ) {
                            editor.performUndo()
                        }
                    }
                    TooltipBox("redo") { tooltip ->
                        IconButton(
                            Icons.AutoMirrored.Filled.Redo,
                            tooltip,
                            editor.redo.isNotEmpty()
                        ) {
                            editor.performRedo()
                        }
                    }
                },
                trailingContent = {
                    TooltipBox("save") { tooltip ->
                        OutlinedIconButton(
                            onClick = {
                                editor.saveAsync()
                            },
                            enabled = editor.nodes.isNotEmpty(),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Save, tooltip)
                        }
                    }
                }
            ) {
                TooltipBox("search") { tooltip ->
                    IconButton(Icons.Filled.Search, tooltip, editor.nodes.isNotEmpty()) {
                        context.upcoming()
                    }
                }
            }
            LazyColumn(
                contentPadding = padding + PaddingValues(bottom = 80.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            ) {
                items(
                    items = editor.nodes,
                    key = { it.uid },
                    contentType = { it.type },
                ) { node ->
                    Box(
                        Modifier
                            .animateItem()
                            .widthIn(max = 512.dp)
                            .padding(start = (node.depth * 16).dp)
                    ) {
                        node.Content(
                            Modifier.combinedClickable(
                                onLongClick = { node.showContextMenu = true }
                            ) {
                                if (node is RootLike && node.parent is NBTNode) {
                                    val expanded = !node.expanded
                                    if (expanded) {
                                        editor.expandNode(node)
                                    } else {
                                        editor.collapsesNode(node)
                                    }
                                    node.expanded = expanded
                                }
                            }
                        )
                        val coroutineScope = rememberCoroutineScope()
                        val clipboard = LocalClipboard.current
                        DropdownMenu(
                            expanded = node.showContextMenu,
                            onDismissRequest = { node.showContextMenu = false },
                            scrollState = rememberScrollState(),
                        ) {
                            DropdownMenuItem(
                                Icons.Filled.ContentCopy,
                                stringResource(R.string.edit_copy)
                            ) {
                                node.showContextMenu = false
                                coroutineScope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(
                                            ClipData.newPlainText(
                                                null,
                                                node.stringify()
                                            )
                                        )
                                    )
                                }
                            }
                            node.ContextMenu(editor)
                            val parent = node.parent
                            if (parent is RootNode) {
                                if (parent is MapNode) {
                                    DropdownMenuItem(
                                        Icons.Filled.Edit,
                                        stringResource(R.string.edit_rename)
                                    ) {
                                        node.showContextMenu = false
                                        editor.renaming = RenamingRequest(node)
                                    }
                                    DropdownMenuItem(
                                        Icons.Filled.SwapHoriz,
                                        stringResource(R.string.action_replace)
                                    ) {
                                        node.showContextMenu = false
                                        editor.replacement = ReplacementRequest(node)
                                    }
                                } else if (parent is CollectionNode<*, *>) {
                                    if (node !== parent.children.firstOrNull()) {
                                        DropdownMenuItem(
                                            Icons.Filled.MoveUp,
                                            stringResource(R.string.action_move_up)
                                        ) {
                                            node.showContextMenu = false
                                            val children = node.parent.children
                                            val index = children.indexOf(node)
                                            if (index in 1 until children.size) {
                                                editor.performOperation(
                                                    Swap(
                                                        node.parent,
                                                        index - 1,
                                                        index
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    if (node !== parent.children.lastOrNull()) {
                                        DropdownMenuItem(
                                            Icons.Filled.MoveDown,
                                            stringResource(R.string.action_move_dowm)
                                        ) {
                                            node.showContextMenu = false
                                            val children = node.parent.children
                                            val index = children.indexOf(node)
                                            if (index in 0 until children.size - 1) {
                                                editor.performOperation(
                                                    Swap(
                                                        node.parent,
                                                        index,
                                                        index + 1
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    if (parent is ListNode) {
                                        DropdownMenuItem(
                                            Icons.Filled.SwapHoriz,
                                            stringResource(R.string.action_replace)
                                        ) {
                                            node.showContextMenu = false
                                            editor.replacement = ReplacementRequest(node)
                                        }
                                    }
                                }
                                DropdownMenuItem(
                                    Icons.Filled.Delete,
                                    stringResource(R.string.edit_delete)
                                ) {
                                    node.showContextMenu = false
                                    editor.performOperation(Delete(node.parent, node))
                                }
                            } else {
                                DropdownMenuItem(
                                    Icons.Filled.Edit,
                                    stringResource(R.string.edit_rename)
                                ) {
                                    node.showContextMenu = false
                                    editor.renaming = RenamingRequest(node)
                                }
                                DropdownMenuItem(
                                    Icons.Filled.SwapHoriz,
                                    stringResource(R.string.action_replace)
                                ) {
                                    node.showContextMenu = false
                                    editor.replacement = ReplacementRequest(node)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    when (val parent = editor.insertion?.parent) {
        is MapNode -> {
            NBTPickerDialog(
                title = stringResource(R.string.action_insert),
                validator = { parent.nodes.containsKey(it.toString()) },
                onCancel = { editor.insertion = null }
            ) { name, tag ->
                editor.insertion = null
                editor.performOperation(
                    Insert(
                        parent,
                        tag.buildNode(
                            parent,
                            name
                        )
                    )
                )
            }
        }

        is CollectionNode<*, *> -> {
            TagPickerDialog(
                title = stringResource(R.string.action_insert),
                initial = parent.children.getHomogenousTypeId(NBTNode::type).toInt(),
                onCancel = { editor.insertion = null }
            ) {
                editor.insertion = null
                editor.performOperation(
                    Insert(
                        parent,
                        it.buildNode(
                            parent,
                            parent.children.size
                        )
                    )
                )
            }
        }

        null -> {}
    }
    val replacement = editor.replacement?.node
    if (replacement !== null) {
        TagPickerDialog(
            title = stringResource(R.string.action_replace),
            initial = replacement.type.toInt(),
            source = replacement,
            exclude = true,
            onCancel = { editor.replacement = null }
        ) {
            editor.replacement = null
            editor.performOperation(
                Replace(
                    replacement.parent as? RootNode,
                    replacement,
                    it.buildNode(
                        replacement.parent,
                        replacement.key
                    )
                )
            )
        }
    }
    val renaming = editor.renaming?.node
    if (renaming !== null) {
        val name = rememberTextFieldState(renaming.key.toString())
        val duplicate = rememberSaveable { mutableStateOf(false) }
        PastableDialog(
            title = stringResource(R.string.rename),
            onPaste = {
                val text = it?.collectText()
                if (text === null) {
                    context.toast(R.string.toast_empty_clipboard)
                } else {
                    name.setTextAndPlaceCursorAtEnd(text)
                }
            },
            onCancel = { editor.renaming = null },
            onConfirm = {
                val key = name.text.toString()
                if (renaming.parent is MapNode) {
                    if (!renaming.parent.nodes.containsKey(key)) {
                        editor.renaming = null
                        editor.performOperation(
                            Rename(renaming.parent, renaming.key.toString(), key)
                        )
                    }
                } else {
                    editor.renaming = null
                    if (key != renaming.key) {
                        editor.performOperation(
                            Rename(null, renaming.key.toString(), key)
                        )
                    }
                }
            },
            isValid = !duplicate.value
        ) {
            TagNameInputField(name, duplicate) {
                renaming.parent is MapNode && renaming.parent.nodes.containsKey(it.toString())
            }
        }
    }
    AnimatedBottomSheetDialog(
        targetState = editor.exporter,
        enabledValues = HiddenOrExpanded
    ) { sheetState, exporter ->
        NBTExportDialog(
            exporter = exporter,
            creator = creator,
            state = sheetState,
            onDismiss = {
                editor.exporter = null
            }
        ) { file ->
            editor.viewModelScope.launch {
                editor.saveToFile(file)
            }
        }
    }
    AnimatedBottomSheetDialog(
        targetState = editor.importer,
        enabledValues = HiddenOrExpanded
    ) { sheetState, importer ->
        NBTImportDialog(
            importer = importer,
            state = sheetState,
            onDismiss = {
                editor.importer = null
            }
        ) {
            editor.viewModelScope.launch {
                editor.readFromFile(importer.source, importer)
            }
        }
    }
}