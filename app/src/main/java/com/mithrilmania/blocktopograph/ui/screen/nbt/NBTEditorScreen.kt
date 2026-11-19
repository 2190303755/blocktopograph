package com.mithrilmania.blocktopograph.ui.screen.nbt

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_DETECT_HEADER
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_NBT_FORMAT
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_SKIP_IMPORTER
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.nbt.toTagType
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import com.mithrilmania.blocktopograph.nbt.util.getHomogenousTypeId
import com.mithrilmania.blocktopograph.nbt.util.parseSNBT
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.AppNavKey.NBTEditor
import com.mithrilmania.blocktopograph.ui.NBTExportDialog
import com.mithrilmania.blocktopograph.ui.NBTImportDialog
import com.mithrilmania.blocktopograph.ui.NBTImportModel
import com.mithrilmania.blocktopograph.ui.NBTPickerDialog
import com.mithrilmania.blocktopograph.ui.TagNameInputField
import com.mithrilmania.blocktopograph.ui.TagPickerDialog
import com.mithrilmania.blocktopograph.ui.component.AlertDialog
import com.mithrilmania.blocktopograph.ui.component.AllSheetValues
import com.mithrilmania.blocktopograph.ui.component.AnimatedBottomSheetDialog
import com.mithrilmania.blocktopograph.ui.component.AnimatedExpanderIndicator
import com.mithrilmania.blocktopograph.ui.component.DragHandleConsumedHeight
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem
import com.mithrilmania.blocktopograph.ui.component.HiddenOrExpanded
import com.mithrilmania.blocktopograph.ui.component.PastableDialog
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.TopAppBar
import com.mithrilmania.blocktopograph.ui.component.cascadingMenu
import com.mithrilmania.blocktopograph.ui.component.clickableItem
import com.mithrilmania.blocktopograph.ui.component.safeLayoutInsets
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.CollectionNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.MapNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.RootNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.buildNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.stringify
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.collectText
import com.mithrilmania.blocktopograph.util.toEnum
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.todoI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun resolveNBTEditor(intent: Intent): NBTEditor? {
    val extras = intent.extras ?: Bundle()
    val uri = intent.data
    val source = if (uri === null) {
        ShizukuFile(extras.getString(EXTRA_PATH) ?: return null)
    } else {
        SAFFile(uri)
    }
    val format = extras.getString(EXTRA_EDITOR_NBT_FORMAT)
        ?.uppercase()
        .toEnum(NBTFormat.UNKNOWN)
    return NBTEditor(
        source to NBTImportConfigImpl(
            format,
            if (format.isHeaderAvailable
                && extras.getBoolean(EXTRA_EDITOR_DETECT_HEADER, true)
            ) null else false
        ),
        extras.getBoolean(EXTRA_EDITOR_SKIP_IMPORTER, false)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBTEditorScreen(
    spec: AppNavKey.NBTEditor,
    onNew: () -> Unit,
    onExit: () -> Unit,
    factory: (Context) -> NBTEditorModel = { NBTEditorModel(it, spec) }
) {
    val context = LocalContext.current
    val editor = viewModel { factory(context) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) callback@{
        editor.importer = NBTImportModel(SAFFile(it ?: return@callback))
    }
    val creator = rememberLauncherForActivityResult(FileCreator) callback@{
        val file = SAFFile(it ?: return@callback)
        editor.viewModelScope.launch {
            editor.saveToFile(file)
            editor.confirmAction()
        }
    }
    BackHandler(editor.modified) {
        editor.confirmation = onExit
    }
    if (editor.confirmation !== null) {
        AlertDialog(
            onDismissRequest = {
                editor.confirmation = null
            },
            title = { Text("更改未保存".todoI18n()) },
            neutralButton = {
                TextButton("继续编辑".todoI18n()) {
                    editor.confirmation = null
                }
            },
            positiveButton = {
                TextButton(stringResource(R.string.action_file_save)) {
                    val source = editor.source
                    if (source === null) {
                        editor.buildExporter()
                    } else {
                        editor.viewModelScope.launch {
                            editor.saveToFile(source)
                            editor.confirmAction()
                        }
                    }
                }
            },
            negativeButton = {
                TextButton("不保存".todoI18n()) {
                    editor.confirmAction()
                }
            }
        ) {
            Text("如果不保存，您的更改将丢失".todoI18n())
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val cutout = safeLayoutInsets()
    val scaffoldState = rememberBottomSheetScaffoldState(
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = AllSheetValues,
        ) { it != SheetValue.Hidden || editor.focused === null }
    )
    LaunchedEffect(editor.focused) {
        if (editor.focused === null) {
            scaffoldState.bottomSheetState.hide()
        } else {
            scaffoldState.bottomSheetState.expand()
        }
    }
    BottomSheetScaffold(
        sheetContent = {
            var showActions by rememberSaveable(editor.focused) {
                mutableStateOf(false)
            }
            AnimatedContent(
                targetState = editor.focused,
                transitionSpec = {
                    val spec = fadeIn() togetherWith fadeOut()
                    if (editor.focused !== null
                        && scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                    ) spec else {
                        spec using SizeTransform { _, _ -> snap() }
                    }
                }
            ) { focused ->
                if (focused === null) {
                    Box(Modifier.fillMaxWidth())
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier
                                .padding(top = 6.dp)
                                .size(width = 32.dp, height = 4.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                .align(Alignment.CenterHorizontally)
                        )
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
                                    text = StringBuilder()
                                        .appendSafeLiteral(focused.key.toString())
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
                                                    ClipData.newPlainText(
                                                        null,
                                                        withContext(Dispatchers.Default) {
                                                            focused.stringify()
                                                        }
                                                    ).toClipEntry()
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
                                        TooltipBox("Options".todoI18n()) { tooltip ->
                                            SplitButtonDefaults.TonalTrailingButton(
                                                checked = showActions,
                                                onCheckedChange = { showActions = it },
                                                modifier =
                                                    Modifier.semantics {
                                                        stateDescription =
                                                            (if (showActions) "Expanded" else "Collapsed")
                                                                .todoI18n()
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
                                    onDismissRequest = { showActions = false },
                                    shape = MenuDefaults.standaloneGroupShape
                                ) {
                                    if (focused.parent is CollectionNode<*, *>) {
                                        DropdownMenuItem(
                                            icon = Icons.Filled.MoveUp,
                                            label = stringResource(R.string.action_move_up),
                                            shape = MenuDefaults.leadingItemShape,
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
                                            label = stringResource(R.string.rename),
                                            shape = MenuDefaults.leadingItemShape
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
                                            onClick = {
                                                if (focused.parent is RootNode) {
                                                    editor.performOperation(
                                                        Delete(focused.parent, focused)
                                                    )
                                                }
                                                showActions = false
                                            },
                                            shape = MenuDefaults.trailingItemShape,
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = label,
                                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                                                )
                                            },
                                            enabled = focused.parent is RootNode,
                                            colors = MenuDefaults.itemColors(
                                                textColor = warn,
                                                leadingIconColor = warn
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        focused.Editor(editor, scaffoldState.bottomSheetState)
                    }
                }
            }
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = LocalDensity.current.run {
            cutout.getBottom(this).toDp()
        } + DragHandleConsumedHeight,
        sheetDragHandle = null,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        editor.source?.resolveName(context)
                            ?: stringResource(R.string.nbt_editor),
                        Modifier.horizontalScroll(rememberScrollState())
                    )
                },
                subtitle = editor.storageVersion?.let { version ->
                    {
                        Text(
                            stringResource(
                                R.string.activity_nbt_editor_subtitle,
                                version.toUInt().toLong()
                            )
                        )
                    }
                },
                actions = {
                    val resources = LocalResources.current
                    AppBarRow(maxItemCount = 4) {
                        clickableItem(
                            Icons.AutoMirrored.Filled.Undo,
                            "Undo".todoI18n(),
                            editor.undo.isNotEmpty(),
                            editor::performUndo
                        )
                        clickableItem(
                            Icons.AutoMirrored.Filled.Redo,
                            "Redo".todoI18n(),
                            editor.redo.isNotEmpty(),
                            editor::performRedo
                        )
                        cascadingMenu(
                            Icons.Filled.Inventory2,
                            resources.getString(R.string.action_file)
                        ) { showMenu ->
                            DropdownMenuItem(
                                resources.getString(R.string.action_file_create),
                                shape = MenuDefaults.leadingItemShape
                            ) {
                                editor.requestOrExecute(onNew)
                                showMenu.value = false
                            }
                            DropdownMenuItem(resources.getString(R.string.action_file_open)) {
                                editor.requestOrExecute {
                                    picker.launch("*/*")
                                }
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
                                editor.source !== null,
                                shape = MenuDefaults.trailingItemShape
                            ) {
                                editor.requestOrExecute {
                                    editor.reload()
                                }
                                showMenu.value = false
                            }
                        }
                        clickableItem(
                            Icons.Filled.Info,
                            resources.getString(R.string.action_file_info),
                            editor.source !== null
                        ) {
                            editor.showProperties = true
                        }
                        clickableItem(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            resources.getString(R.string.action_quit)
                        ) {
                            editor.requestOrExecute(onExit)
                        }
                    }
                }
            )
        }
    ) { padding ->
        NBTTree(
            padding,
            editor,
            scaffoldState.bottomSheetState,
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        )
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
    if (editor.showProperties) {
        editor.source?.let { source ->
            val onDismissRequest = {
                editor.showProperties = false
            }
            AlertDialog(
                onDismissRequest = onDismissRequest,
                title = {
                    Text(stringResource(R.string.action_file_info))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 3),
                            supportingContent = {
                                SelectionContainer {
                                    Text(source.describeSource())
                                }
                            }
                        ) { Text("数据源".todoI18n()) }
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(index = 1, count = 3),
                            supportingContent = {
                                Text(
                                    if (editor.stringify) "SNBT".todoI18n()
                                    else if (editor.littleEndian) "小端序".todoI18n()
                                    else "大端序".todoI18n()
                                )
                            }
                        ) { Text("格式".todoI18n()) }
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(index = 2, count = 3),
                            supportingContent = {
                                Text(if (editor.compress) "是".todoI18n() else "否".todoI18n())
                            }
                        ) { Text("压缩".todoI18n()) }
                    }
                },
                confirmButton = {
                    TextButton(stringResource(android.R.string.ok), onClick = onDismissRequest)
                }
            )
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
