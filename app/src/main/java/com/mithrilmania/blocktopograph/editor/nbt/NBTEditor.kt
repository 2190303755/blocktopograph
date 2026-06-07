package com.mithrilmania.blocktopograph.editor.nbt

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.node.CollectionNode
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.editor.nbt.node.buildNode
import com.mithrilmania.blocktopograph.editor.nbt.node.stringify
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.toTagType
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import com.mithrilmania.blocktopograph.nbt.util.getHomogenousTypeId
import com.mithrilmania.blocktopograph.nbt.util.parseSNBT
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.ui.component.AlertDialog
import com.mithrilmania.blocktopograph.ui.component.AnimatedBottomSheetDialog
import com.mithrilmania.blocktopograph.ui.component.AnimatedExpanderIndicator
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem
import com.mithrilmania.blocktopograph.ui.component.HiddenOrExpanded
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

@Composable
fun NBTSummary(
    expandable: RootNode?,
    icon: Painter,
    key: String,
    summary: String,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = icon, contentDescription = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (expandable !== null) {
            AnimatedExpanderIndicator(
                expandable.expanded,
                SplitButtonDefaults.TrailingIconSize
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    AppBarRow(maxItemCount = 4) {
                        clickableItem(
                            Icons.AutoMirrored.Filled.Undo,
                            "Undo", // TODO: i18n
                            editor.undo.isNotEmpty(),
                            editor::performUndo
                        )
                        clickableItem(
                            Icons.AutoMirrored.Filled.Redo,
                            "Redo", // TODO: i18n
                            editor.redo.isNotEmpty(),
                            editor::performRedo
                        )
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
                            ) {
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
        Column(Modifier.fillMaxSize()) {
            NBTTree(padding, editor, Modifier.weight(1F))
            var showActions by rememberSaveable(editor.focused) {
                mutableStateOf(false)
            }
            AnimatedContent(
                targetState = editor.focused,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { focused ->
                if (focused === null) {
                    Box(Modifier)
                } else {
                    Column(Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                painter = focused.icon(),
                                contentDescription = null
                            )
                            val root = editor.nodes.firstOrNull()
                            if (focused === root) {
                                Text(
                                    text = StringBuilder().appendSafeLiteral(focused.key.toString())
                                        .toString(),
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .weight(1F)
                                        .horizontalScroll(rememberScrollState(Int.MAX_VALUE))
                                )
                            } else {
                                Text(
                                    text = focused.path,
                                    modifier = Modifier
                                        .weight(1F)
                                        .horizontalScroll(rememberScrollState(Int.MAX_VALUE))
                                )
                            }
                            Box {
                                val context = LocalContext.current
                                val clipboard = LocalClipboard.current
                                val coroutineScope = rememberCoroutineScope()
                                SplitButtonLayout(
                                    leadingButton = {
                                        SplitButtonDefaults.TonalLeadingButton(onClick = {
                                            coroutineScope.launch {
                                                clipboard.setClipEntry(
                                                    ClipEntry(
                                                        ClipData.newPlainText(
                                                            null,
                                                            focused.stringify()
                                                        )
                                                    )
                                                )
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                                contentDescription = stringResource(R.string.action_copy),
                                            )
                                        }
                                    },
                                    trailingButton = {
                                        TooltipBox("Toggle Button") { tooltip ->
                                            SplitButtonDefaults.TonalTrailingButton(
                                                checked = showActions,
                                                onCheckedChange = { showActions = it },
                                                modifier =
                                                    Modifier.semantics {
                                                        stateDescription =
                                                            if (showActions) "Expanded" else "Collapsed"
                                                        contentDescription = tooltip
                                                    },
                                            ) {
                                                AnimatedExpanderIndicator(
                                                    showActions,
                                                    SplitButtonDefaults.TrailingIconSize
                                                )
                                            }
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = showActions,
                                    onDismissRequest = { showActions = false }
                                ) {
                                    if (focused.parent is CollectionNode<*, *>) {
                                        DropdownMenuItem(
                                            icon = Icons.Filled.MoveUp,
                                            label = stringResource(R.string.action_move_up),
                                            enabled = focused !== focused.parent.children.firstOrNull()
                                        ) {
                                            val children = focused.parent.children
                                            val index = children.indexOf(focused)
                                            if (index in 1 until children.size) {
                                                editor.performOperation(
                                                    Swap(
                                                        focused.parent,
                                                        index - 1,
                                                        index
                                                    )
                                                )
                                            } else {
                                                showActions = false
                                            }
                                        }
                                        DropdownMenuItem(
                                            icon = Icons.Filled.MoveDown,
                                            label = stringResource(R.string.action_move_dowm),
                                            enabled = focused !== focused.parent.children.lastOrNull()
                                        ) {
                                            val children = focused.parent.children
                                            val index = children.indexOf(focused)
                                            if (index in 0 until children.size - 1) {
                                                editor.performOperation(
                                                    Swap(
                                                        focused.parent,
                                                        index,
                                                        index + 1
                                                    )
                                                )
                                            } else {
                                                showActions = false
                                            }
                                        }
                                    } else {
                                        DropdownMenuItem(
                                            icon = Icons.Filled.Edit,
                                            label = stringResource(R.string.rename)
                                        ) {
                                            editor.renaming = RenamingRequest(focused)
                                            showActions = false
                                        }
                                    }
                                    DropdownMenuItem(
                                        icon = Icons.Filled.SwapHoriz,
                                        label = stringResource(R.string.action_replace)
                                    ) {
                                        if (focused.parent.canBeHeterogeneous) {
                                            editor.replacing = ReplacingRequest(focused)
                                        } else {
                                            coroutineScope.launch {
                                                val tag = clipboard.getClipEntry()
                                                    ?.clipData
                                                    ?.collectText()
                                                    ?.parseSNBT()
                                                    ?.second
                                                if (tag === null) {
                                                    context.toast(R.string.clipboard_is_empty) // fixme: invalid snbt
                                                } else {
                                                    editor.performOperation(
                                                        Replace(
                                                            focused.parent as? RootNode,
                                                            focused,
                                                            focused.type
                                                                .toTagType()
                                                                .transform(tag)
                                                                .buildNode(
                                                                    focused.parent,
                                                                    focused.key
                                                                )
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        showActions = false
                                    }
                                    HorizontalDivider()
                                    val warn = MaterialTheme.colorScheme.error
                                    CompositionLocalProvider(
                                        LocalContentColor provides warn
                                    ) {
                                        val label = stringResource(R.string.action_delete)
                                        DropdownMenuItem(
                                            text = { Text(text = label) },
                                            enabled = focused.parent is RootNode,
                                            colors = MenuDefaults.itemColors(
                                                textColor = warn,
                                                leadingIconColor = warn
                                            ),
                                            onClick = {
                                                if (focused.parent is RootNode) {
                                                    editor.performOperation(
                                                        Delete(focused.parent, focused)
                                                    )
                                                }
                                                showActions = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = label,
                                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        focused.Editor(editor)
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
                    Insert(parent, tag.buildNode(parent, name))
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
                    Insert(parent, it.buildNode(parent, parent.children.size))
                )
            }
        }

        null -> {}
    }
    val replacing = editor.replacing?.node
    if (replacing !== null && replacing.parent.canBeHeterogeneous) {
        TagPickerDialog(
            title = stringResource(R.string.action_replace),
            initial = replacing.type.toInt(),
            source = replacing,
            exclude = true,
            onCancel = { editor.replacing = null }
        ) {
            editor.replacing = null
            editor.performOperation(
                Replace(
                    replacing.parent as? RootNode,
                    replacing,
                    it.buildNode(
                        replacing.parent,
                        replacing.key
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
                } else if (renaming.parent !is NBTNode) {
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
            onDismiss = { editor.exporter = null }
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
            onDismiss = { editor.importer = null }
        ) {
            editor.viewModelScope.launch {
                editor.readFromFile(importer.source, importer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NBTTree(
    padding: PaddingValues,
    editor: NBTEditorModel,
    modifier: Modifier
) {
    Box(modifier) {
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
        val itemShape = MaterialTheme.shapes.small
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            items(
                items = editor.nodes,
                key = { it.uid },
                contentType = { it.type },
            ) { node ->
                val indent = Modifier
                    .padding(start = (node.depth * 32 - 16).dp)
                NBTSummary(
                    expandable = node as? RootNode,
                    icon = node.icon(),
                    key = node.key.toString(),
                    summary = node.summary(),
                    modifier = (if (node === editor.focused) indent.background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        itemShape
                    ) else indent)
                        .clip(itemShape)
                        .clickable {
                            if (editor.focused === node
                                && node is RootNode
                                && node.parent is NBTNode
                            ) {
                                val expanded = !node.expanded
                                if (expanded) {
                                    editor.expandNode(node)
                                } else {
                                    editor.collapsesNode(node)
                                }
                                node.expanded = expanded
                            }
                            editor.focused = node
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .animateItem()
                )
            }
        }
    }
}
