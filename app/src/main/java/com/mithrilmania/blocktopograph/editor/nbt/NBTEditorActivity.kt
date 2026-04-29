package com.mithrilmania.blocktopograph.editor.nbt

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_DEFAULT_FORMAT
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_DETECT_HEADER
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_SKIP_IMPORTER
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.dialog.NBTExportDialog
import com.mithrilmania.blocktopograph.editor.dialog.NBTImportDialog
import com.mithrilmania.blocktopograph.editor.dialog.NBTImportModel
import com.mithrilmania.blocktopograph.editor.dialog.NBTPickerDialog
import com.mithrilmania.blocktopograph.editor.dialog.TagNameInputField
import com.mithrilmania.blocktopograph.editor.dialog.TagPickerDialog
import com.mithrilmania.blocktopograph.editor.nbt.node.CollectionNode
import com.mithrilmania.blocktopograph.editor.nbt.node.ListNode
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootLike
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.editor.nbt.node.buildNode
import com.mithrilmania.blocktopograph.editor.nbt.node.stringify
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.util.getHomogenousTypeId
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.ui.component.AlertDialog
import com.mithrilmania.blocktopograph.ui.component.AnimatedBottomSheetDialog
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.PastableDialog
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.TopAppBar
import com.mithrilmania.blocktopograph.ui.component.cascadingMenu
import com.mithrilmania.blocktopograph.ui.component.clickableItem
import com.mithrilmania.blocktopograph.ui.theme.setThemedContent
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.FilePicker
import com.mithrilmania.blocktopograph.util.collectText
import com.mithrilmania.blocktopograph.util.setPrimaryClip
import com.mithrilmania.blocktopograph.util.toEnum
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.upcoming
import kotlinx.coroutines.launch

class NBTEditorActivity : ComponentActivity() {
    private val viewModel by viewModels<NBTEditorModel>()
    private lateinit var open: ActivityResultLauncher<Uri?>
    private lateinit var create: ActivityResultLauncher<FileCreator.Options?>

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        this.open = registerForActivityResult(FilePicker) callback@{
            this.viewModel.importer = NBTImportModel(SAFFile(it ?: return@callback))
        }
        this.create = registerForActivityResult(FileCreator) callback@{
            val file = SAFFile(it ?: return@callback)
            this.viewModel.viewModelScope.launch {
                saveToFile(file)
                executeConfirmation()
            }
        }
        if (savedInstanceState === null) {
            this.onNewIntent(this.intent)
        }
        this.setThemedContent {
            val viewModel = this.viewModel
            BackHandler(viewModel.modified) {
                viewModel.confirmation = ConfirmationRequest.EXIT
            }
            if (viewModel.confirmation !== null) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.confirmation = null
                    },
                    title = { Text("更改未保存") },
                    neutralButton = {
                        TextButton("继续编辑") {
                            viewModel.confirmation = null
                        }
                    },
                    positiveButton = {
                        TextButton("保存") {
                            val source = viewModel.source
                            if (source === null) {
                                viewModel.buildExporter()
                            } else {
                                viewModel.viewModelScope.launch {
                                    saveToFile(source)
                                    executeConfirmation()
                                }
                            }
                        }
                    },
                    negativeButton = {
                        TextButton("不保存", onClick = this::executeConfirmation)
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
                                viewModel.source?.getName(this@NBTEditorActivity)
                                    ?: stringResource(R.string.nbt_editor)
                            )
                        },
                        subtitle = viewModel.storageVersion?.let { version ->
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
                            AppBarRow(maxItemCount = 2) {
                                cascadingMenu(
                                    Icons.Filled.Inventory2,
                                    getString(R.string.action_file)
                                ) { showMenu ->
                                    DropdownMenuItem(getString(R.string.action_file_create)) {
                                        if (viewModel.modified) {
                                            viewModel.confirmation = ConfirmationRequest.NEW
                                        } else {
                                            viewModel.reset()
                                        }
                                        showMenu.value = false
                                    }
                                    DropdownMenuItem(getString(R.string.action_file_open)) {
                                        if (viewModel.modified) {
                                            viewModel.confirmation = ConfirmationRequest.OPEN
                                        } else {
                                            open.launch(null)
                                        }
                                        showMenu.value = false
                                    }
                                    DropdownMenuItem(
                                        getString(R.string.action_file_save),
                                        viewModel.nodes.isNotEmpty()
                                    ) {
                                        saveAsync()
                                        showMenu.value = false
                                    }
                                    DropdownMenuItem(
                                        getString(R.string.action_file_save_as),
                                        viewModel.nodes.isNotEmpty()
                                    ) {
                                        viewModel.buildExporter(true)
                                        showMenu.value = false
                                    }
                                    DropdownMenuItem(
                                        getString(R.string.action_file_reload),
                                        viewModel.source !== null
                                    ) click@{
                                        if (viewModel.modified) {
                                            viewModel.confirmation = ConfirmationRequest.RELOAD
                                        } else {
                                            this@NBTEditorActivity.viewModel.importer =
                                                NBTImportModel(viewModel.source ?: return@click)
                                        }
                                        showMenu.value = false
                                    }
                                }
                                clickableItem(
                                    Icons.Filled.Info,
                                    getString(R.string.action_file_info),
                                    false
                                ) {
                                    upcoming()
                                }
                                clickableItem(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    getString(R.string.action_quit)
                                ) {
                                    this@NBTEditorActivity.viewModel.apply {
                                        if (modified) {
                                            confirmation = ConfirmationRequest.EXIT
                                        } else {
                                            this@NBTEditorActivity.finish()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

            ) { padding ->
                Box(Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = viewModel.flattening,
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
                                    viewModel.undo.isNotEmpty()
                                ) {
                                    viewModel.performUndo()
                                }
                            }
                            TooltipBox("redo") { tooltip ->
                                IconButton(
                                    Icons.AutoMirrored.Filled.Redo,
                                    tooltip,
                                    viewModel.redo.isNotEmpty()
                                ) {
                                    viewModel.performRedo()
                                }
                            }
                        },
                        trailingContent = {
                            TooltipBox("save") { tooltip ->
                                OutlinedIconButton(
                                    onClick = {
                                        saveAsync()
                                    },
                                    enabled = viewModel.nodes.isNotEmpty(),
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
                            IconButton(Icons.Filled.Search, tooltip, viewModel.nodes.isNotEmpty()) {
                                this@NBTEditorActivity.upcoming()
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
                            items = viewModel.nodes,
                            key = { it.uid },
                            contentType = { it.type },
                        ) { node ->
                            Box(
                                Modifier
                                    .animateItem()
                                    .padding(start = (node.depth * 16).dp)
                            ) {
                                node.Content(
                                    Modifier.combinedClickable(
                                        onLongClick = { node.showContextMenu = true }
                                    ) {
                                        if (node is RootLike && node.parent is NBTNode) {
                                            val expanded = !node.expanded
                                            if (expanded) {
                                                viewModel.expandNode(node)
                                            } else {
                                                viewModel.collapsesNode(node)
                                            }
                                            node.expanded = expanded
                                        }
                                    }
                                )
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
                                        this@NBTEditorActivity.setPrimaryClip {
                                            ClipData.newPlainText("Copy", node.stringify())
                                        }
                                    }
                                    node.ContextMenu(viewModel)
                                    val parent = node.parent
                                    if (parent is RootNode) {
                                        if (parent is MapNode) {
                                            DropdownMenuItem(
                                                Icons.Filled.Edit,
                                                stringResource(R.string.edit_rename)
                                            ) {
                                                node.showContextMenu = false
                                                viewModel.renaming = RenamingRequest(node)
                                            }
                                            DropdownMenuItem(
                                                Icons.Filled.SwapHoriz,
                                                stringResource(R.string.action_replace)
                                            ) {
                                                node.showContextMenu = false
                                                viewModel.replacement = ReplacementRequest(node)
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
                                                        viewModel.performOperation(
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
                                                        viewModel.performOperation(
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
                                                    viewModel.replacement = ReplacementRequest(node)
                                                }
                                            }
                                        }
                                        DropdownMenuItem(
                                            Icons.Filled.Delete,
                                            stringResource(R.string.edit_delete)
                                        ) {
                                            node.showContextMenu = false
                                            viewModel.performOperation(Delete(node.parent, node))
                                        }
                                    } else {
                                        DropdownMenuItem(
                                            Icons.Filled.Edit,
                                            stringResource(R.string.edit_rename)
                                        ) {
                                            node.showContextMenu = false
                                            viewModel.renaming = RenamingRequest(node)
                                        }
                                        DropdownMenuItem(
                                            Icons.Filled.SwapHoriz,
                                            stringResource(R.string.action_replace)
                                        ) {
                                            node.showContextMenu = false
                                            viewModel.replacement = ReplacementRequest(node)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            when (val parent = viewModel.insertion?.parent) {
                is MapNode -> {
                    NBTPickerDialog(
                        title = stringResource(R.string.action_insert),
                        validator = { parent.nodes.containsKey(it.toString()) },
                        onCancel = { viewModel.insertion = null }
                    ) { name, tag ->
                        viewModel.insertion = null
                        viewModel.performOperation(
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
                        onCancel = { viewModel.insertion = null }
                    ) {
                        viewModel.insertion = null
                        viewModel.performOperation(
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
            val replacement = viewModel.replacement?.node
            if (replacement !== null) {
                TagPickerDialog(
                    title = stringResource(R.string.action_replace),
                    initial = replacement.type.toInt(),
                    source = replacement,
                    exclude = true,
                    onCancel = { viewModel.replacement = null }
                ) {
                    viewModel.replacement = null
                    viewModel.performOperation(
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
            val renaming = viewModel.renaming?.node
            if (renaming !== null) {
                val name = rememberTextFieldState(renaming.key.toString())
                val duplicate = rememberSaveable { mutableStateOf(false) }
                PastableDialog(
                    title = stringResource(R.string.rename),
                    onPaste = {
                        val text = it?.collectText()
                        if (text === null) {
                            toast(R.string.toast_empty_clipboard)
                        } else {
                            name.setTextAndPlaceCursorAtEnd(text)
                        }
                    },
                    onCancel = { viewModel.renaming = null },
                    onConfirm = {
                        val key = name.text.toString()
                        if (renaming.parent is MapNode) {
                            if (!renaming.parent.nodes.containsKey(key)) {
                                viewModel.renaming = null
                                viewModel.performOperation(
                                    Rename(renaming.parent, renaming.key.toString(), key)
                                )
                            }
                        } else {
                            viewModel.renaming = null
                            if (key != renaming.key) {
                                viewModel.performOperation(
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
                targetState = viewModel.exporter,
                skipPartiallyExpanded = true
            ) { sheetState, exporter ->
                NBTExportDialog(
                    exporter = exporter,
                    picker = this.create,
                    state = sheetState,
                    onDismiss = {
                        viewModel.exporter = null
                    }
                ) { file ->
                    viewModel.viewModelScope.launch {
                        saveToFile(file)
                    }
                }
            }
            AnimatedBottomSheetDialog(
                targetState = viewModel.importer,
                skipPartiallyExpanded = true
            ) { sheetState, importer ->
                NBTImportDialog(
                    importer = importer,
                    state = sheetState,
                    onDismiss = {
                        viewModel.importer = null
                    }
                ) {
                    viewModel.viewModelScope.launch {
                        readAsync(importer)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                val importer = NBTImportModel(
                    if (uri == null) {
                        ShizukuFile(intent.getStringExtra(EXTRA_PATH) ?: return)
                    } else {
                        SAFFile(uri)
                    },
                    header = intent.getBooleanExtra(EXTRA_EDITOR_DETECT_HEADER, true),
                    format = intent.getStringExtra(EXTRA_EDITOR_DEFAULT_FORMAT)
                        ?.uppercase()
                        .toEnum(NBTFormat.UNKNOWN)
                )
                if (intent.getBooleanExtra(EXTRA_EDITOR_SKIP_IMPORTER, false)) {
                    this.readAsync(importer)
                } else {
                    this.viewModel.importer = importer
                }
            }
        }
    }

    fun readAsync(importer: NBTImportModel) {
        viewModel.viewModelScope.launch {
            viewModel.readFromFile(importer, this@NBTEditorActivity)
        }
    }


    fun saveAsync() {
        val source = viewModel.source
        if (source === null) {
            viewModel.buildExporter()
        } else {
            viewModel.viewModelScope.launch {
                saveToFile(source)
            }
        }
    }

    suspend fun saveToFile(file: File) {
        this.viewModel.saveToFile(file, this)
    }

    fun executeConfirmation() {
        val request = this.viewModel.confirmation
        this.viewModel.confirmation = null
        when (request) {
            ConfirmationRequest.EXIT -> this.finish()
            ConfirmationRequest.NEW -> this.viewModel.reset()
            ConfirmationRequest.RELOAD -> this.viewModel.apply {
                importer = NBTImportModel(
                    source = source ?: return,
                    header = storageVersion != null,
                    format = if (stringify) {
                        NBTFormat.STRINGIFIED
                    } else if (littleEndian) {
                        NBTFormat.LITTLE_ENDIAN
                    } else {
                        NBTFormat.BIG_ENDIAN
                    }
                )
            }

            ConfirmationRequest.OPEN -> this.open.launch(null)
            else -> {}
        }
    }
}