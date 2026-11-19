package com.mithrilmania.blocktopograph.ui.screen.settings

import android.content.ClipData
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.isShizukuAvailable
import com.mithrilmania.blocktopograph.settings.FlagStore
import com.mithrilmania.blocktopograph.settings.ScanningStore
import com.mithrilmania.blocktopograph.settings.asFlagState
import com.mithrilmania.blocktopograph.settings.asPathsFlow
import com.mithrilmania.blocktopograph.storage.JvmFile
import com.mithrilmania.blocktopograph.storage.awaitFileService
import com.mithrilmania.blocktopograph.storage.isLegacyExternalStorageAvailable
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.component.AnimatedBottomSheetDialog
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem
import com.mithrilmania.blocktopograph.ui.component.FloatingActionScaffold
import com.mithrilmania.blocktopograph.ui.component.HiddenOrExpanded
import com.mithrilmania.blocktopograph.ui.component.PastableDialog
import com.mithrilmania.blocktopograph.ui.component.SwipeableSnackbar
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.awaitSnackbar
import com.mithrilmania.blocktopograph.ui.component.excludeTop
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.util.FLAG_GRANT_ALL_URI_PERMISSION
import com.mithrilmania.blocktopograph.util.collectText
import com.mithrilmania.blocktopograph.util.rpc
import com.mithrilmania.blocktopograph.util.runSuppressing
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.tryOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class InputState {
    UNCHECKED,
    EMPTY,
    RESOLVING,
    UNRESOLVABLE
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PathManager(padding: PaddingValues) {
    var deleting by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val paths = remember {
        ScanningStore.PATHS.asPathsFlow(context).map(Set<String>::sorted)
    }.collectAsStateWithLifecycle(null)
    val colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding.excludeTop() + PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        val list = paths.value
        if (list === null) {
            item {
                Box(
                    Modifier
                        .fillParentMaxSize()
                        .animateItem()
                ) {
                    LoadingIndicator(Modifier.align(Alignment.Center))
                }
            }
        } else {
            itemsIndexed(list, key = { _, path -> path }) { index, path ->
                var expended by remember { mutableStateOf(false) }
                SegmentedListItem(
                    selected = expended,
                    onClick = { expended = true },
                    shapes = ListItemDefaults.segmentedShapes(index, list.size),
                    colors = colors,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.animateItem(),
                    content = {
                        Text(text = path, modifier = Modifier.fillMaxWidth())
                        DropdownMenuPopup(
                            expanded = expended,
                            onDismissRequest = { expended = false },
                            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                                MenuAnchorPosition.End
                            )
                        ) {
                            DropdownMenuGroup(MenuDefaults.groupShape(0, 2)) {
                                val clipboard = LocalClipboard.current
                                DropdownMenuItem(
                                    icon = Icons.Filled.ContentCopy,
                                    label = stringResource(R.string.action_copy_path),
                                    shape = MenuDefaults.leadingItemShape
                                ) {
                                    coroutineScope.launch {
                                        clipboard.setClipEntry(
                                            ClipData.newPlainText("Path", path)
                                                .toClipEntry()
                                        )
                                        context.toast(R.string.toast_copy_success)
                                    }
                                    expended = false
                                }
                            }
                            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                            DropdownMenuGroup(MenuDefaults.groupShape(1, 2)) {
                                val color = MaterialTheme.colorScheme.error
                                CompositionLocalProvider(LocalContentColor provides color) {
                                    DropdownMenuItem(
                                        icon = Icons.Filled.Delete,
                                        label = stringResource(R.string.action_remove_path),
                                        shape = MenuDefaults.trailingItemShape,
                                        colors = MenuDefaults.itemColors(
                                            textColor = color,
                                            leadingIconColor = color
                                        )
                                    ) {
                                        deleting = path
                                        expended = false
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
    deleting?.let { path ->
        val onDismissRequest = { deleting = null }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(stringResource(R.string.action_remove_path))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.scanning_remove_path_message))
                    Text(path)
                }
            },
            dismissButton = {
                TextButton(stringResource(android.R.string.cancel), onClick = onDismissRequest)
            },
            confirmButton = {
                TextButton(stringResource(android.R.string.ok)) {
                    deleting = null
                    coroutineScope.launch(Dispatchers.IO) {
                        ScanningStore.remove(context, path)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UriManager(
    padding: PaddingValues,
    onRemoveUri: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    val uris = remember {
        ScanningStore.URIS.asPathsFlow(appContext).map(Set<String>::sorted)
    }.collectAsStateWithLifecycle(null, minActiveState = Lifecycle.State.CREATED)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding.excludeTop() + PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        val list = uris.value
        if (list === null) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .animateItem(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        } else if (list.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .animateItem(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.scanning_empty_uris),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(list, key = { _, uri -> uri }) { index, uriString ->
                var expended by remember { mutableStateOf(false) }
                SegmentedListItem(
                    selected = expended,
                    onClick = { expended = true },
                    shapes = ListItemDefaults.segmentedShapes(index, list.size),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.animateItem(),
                    supportingContent = { Text(uriString) },
                    content = {
                        Text(
                            text = uriString.toUri().lastPathSegment ?: uriString,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenuPopup(
                            expanded = expended,
                            onDismissRequest = { expended = false },
                            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                                MenuAnchorPosition.End
                            )
                        ) {
                            DropdownMenuGroup(MenuDefaults.groupShape(0, 2)) {
                                val clipboard = LocalClipboard.current
                                DropdownMenuItem(
                                    icon = Icons.Filled.ContentCopy,
                                    label = stringResource(R.string.action_copy_uri),
                                    shape = MenuDefaults.leadingItemShape
                                ) {
                                    coroutineScope.launch {
                                        clipboard.setClipEntry(
                                            ClipData.newPlainText("URI", uriString).toClipEntry()
                                        )
                                        appContext.toast(R.string.toast_copy_success)
                                    }
                                    expended = false
                                }
                            }
                            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                            DropdownMenuGroup(MenuDefaults.groupShape(1, 2)) {
                                val color = MaterialTheme.colorScheme.error
                                CompositionLocalProvider(LocalContentColor provides color) {
                                    DropdownMenuItem(
                                        icon = Icons.Filled.Delete,
                                        label = stringResource(R.string.action_remove_uri),
                                        shape = MenuDefaults.trailingItemShape,
                                        colors = MenuDefaults.itemColors(
                                            textColor = color,
                                            leadingIconColor = color
                                        )
                                    ) {
                                        onRemoveUri(uriString)
                                        expended = false
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanningManagerScreen() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var addingPath by remember { mutableStateOf(false) }
    var addingUri by remember { mutableStateOf<Unit?>(null) }
    val pagerState = rememberPagerState { 2 }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isShizukuAllowed by FlagStore.IS_SHIZUKU_ALLOWED.asFlagState(context, false)
    val snackbarHostState = remember { SnackbarHostState() }
    val uriRemovedMessage = stringResource(R.string.scanning_uri_removed_retained)
    val releasePermissionLabel = stringResource(R.string.scanning_release_permission)

    fun canAddPath() =
        isLegacyExternalStorageAvailable() ||
                (isShizukuAllowed && isShizukuAvailable())

    // Removing a URI scan location keeps the persisted permission; the snackbar
    // action offers to release it.
    fun removeUriLocation(uriString: String) {
        coroutineScope.launch {
            ScanningStore.remove(context, uriString.toUri())
        }
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.awaitSnackbar(
                message = uriRemovedMessage,
                actionLabel = releasePermissionLabel,
                duration = SnackbarDuration.Long
            ) {
                withContext(Dispatchers.IO) {
                    val uri = uriString.toUri()
                    val flags = context.contentResolver.persistedUriPermissions
                        .firstOrNull { it.uri == uri }
                        ?.let { permission ->
                            (if (permission.isReadPermission)
                                Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                                    (if (permission.isWritePermission)
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
                        } ?: FLAG_GRANT_ALL_URI_PERMISSION
                    runSuppressing {
                        context.contentResolver.releasePersistableUriPermission(uri, flags)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(AppNavKey.ScanningManager.title)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = ::AppBarNavigationButton
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                SwipeableSnackbar(it)
            }
        },
        floatingActionButton = {
            TooltipBox(
                stringResource(
                    if (pagerState.currentPage == 1) R.string.scanning_add_path
                    else R.string.scanning_add_uri
                )
            ) { tooltip ->
                val isVisible = remember {
                    derivedStateOf {
                        when {
                            pagerState.isScrollInProgress -> false
                            pagerState.currentPage != 1 -> true
                            isLegacyExternalStorageAvailable() -> true
                            isShizukuAllowed && isShizukuAvailable() -> true
                            else -> false
                        }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        if (pagerState.currentPage == 1) {
                            // not sure if it is clickable when animating
                            addingPath = canAddPath()
                        } else {
                            addingUri = Unit
                        }
                    },
                    modifier = Modifier.animateFloatingActionButton(
                        visible = isVisible.value,
                        alignment = Alignment.Center
                    )
                ) {
                    Icon(Icons.Filled.AddLocationAlt, tooltip)
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding.onlyTop())) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    text = { Text(stringResource(R.string.scanning_tab_uri)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    text = { Text(stringResource(R.string.scanning_tab_path)) }
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 1) {
                    PathManager(padding)
                } else {
                    UriManager(padding, onRemoveUri = ::removeUriLocation)
                }
            }
        }
        if (addingPath) {
            val textFieldState = rememberTextFieldState()
            var inputState by rememberSaveable { mutableStateOf(InputState.EMPTY) }
            val onInput: (CharSequence) -> Unit = {
                inputState = if (it.isEmpty()) InputState.EMPTY else InputState.UNCHECKED
            }
            LaunchedEffect(Unit) {
                snapshotFlow { textFieldState.text }.collect {
                    onInput(it.trim())
                }
            }
            val onConfirm: () -> Unit = {
                inputState = InputState.RESOLVING
                coroutineScope.launch {
                    val path = textFieldState.text.trim().toString()
                    val resolved = if (isLegacyExternalStorageAvailable()) tryOrNull {
                        JvmFile(path).canonicalPath
                    } else awaitFileService()?.rpc {
                        it.canonicalize(path)
                    }
                    if (inputState != InputState.RESOLVING) return@launch
                    if (resolved === null) {
                        inputState = InputState.UNRESOLVABLE
                    } else {
                        ScanningStore.add(context, resolved)
                        addingPath = false
                    }
                }
            }
            PastableDialog(
                title = stringResource(R.string.scanning_add_path),
                onPaste = {
                    val text = it?.collectText()
                    if (text === null) {
                        context.toast(R.string.toast_empty_clipboard)
                    } else {
                        textFieldState.setTextAndPlaceCursorAtEnd(text)
                    }
                },
                onCancel = { addingPath = false },
                onConfirm = onConfirm,
                isValid = inputState == InputState.UNCHECKED
            ) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                OutlinedTextField(
                    state = textFieldState,
                    modifier = Modifier.focusRequester(focusRequester),
                    shape = OutlinedTextFieldDefaults.roundedShape,
                    isError = inputState != InputState.UNCHECKED && inputState != InputState.RESOLVING,
                    enabled = inputState != InputState.RESOLVING,
                    supportingText = {
                        Column {
                            Text(
                                when (inputState) {
                                    InputState.EMPTY -> stringResource(R.string.scanning_path_empty)
                                    InputState.RESOLVING -> stringResource(R.string.scanning_path_resolving)
                                    InputState.UNRESOLVABLE -> stringResource(R.string.scanning_path_unresolvable)
                                    else -> ""
                                }
                            )
                            if (inputState == InputState.RESOLVING) {
                                LinearProgressIndicator()
                            } else {
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    onKeyboardAction = {
                        onInput(textFieldState.text.trim())
                        if (inputState == InputState.UNCHECKED) {
                            onConfirm()
                            it()
                        }
                    }
                )
            }
        }
        AnimatedBottomSheetDialog(addingUri, HiddenOrExpanded) { state, _ ->
            AddScanUriSheet(state) {
                addingUri = null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScanUriSheet(sheetState: SheetState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    val uris = remember {
        ScanningStore.URIS.asPathsFlow(appContext).map { set ->
            appContext.contentResolver.persistedUriPermissions.filter { permission ->
                permission.isWritePermission && permission.isReadPermission
                        && DocumentsContract.isTreeUri(permission.uri)
            }.map { it.uri.toString() to it.uri }.filterNot { it.first in set }
        }.flowOn(Dispatchers.IO)
    }.collectAsStateWithLifecycle(emptyList())

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri !== null) {
            scope.launch(Dispatchers.IO) {
                if (null !== tryOrNull {
                        appContext.contentResolver.takePersistableUriPermission(
                            uri,
                            FLAG_GRANT_ALL_URI_PERMISSION
                        )
                    }
                ) {
                    ScanningStore.add(appContext, uri)
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        }
    ) {
        FloatingActionScaffold(
            state = sheetState,
            action = {
                SegmentedListItem(
                    selected = true,
                    onClick = { picker.launch(null) },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 24.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                    elevation = ListItemDefaults.elevation(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    trailingContent = { Icon(Icons.Filled.Add, null) },
                ) {
                    Text(stringResource(R.string.scanning_add_from_saf))
                }
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        ) { contentPadding ->
            val colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                itemsIndexed(
                    uris.value,
                    key = { _, uri -> uri }
                ) { index, pair ->
                    val friendly = pair.second.lastPathSegment
                    SegmentedListItem(
                        onClick = {
                            scope.launch {
                                ScanningStore.add(appContext, pair.second)
                                onDismiss()
                            }
                        },
                        shapes = ListItemDefaults.segmentedShapes(
                            index = index,
                            count = uris.value.size
                        ),
                        colors = colors,
                        verticalAlignment = Alignment.CenterVertically,
                        supportingContent = if (friendly === null) null else {
                            { Text(pair.first) }
                        }
                    ) {
                        Text(friendly ?: pair.first)
                    }
                }
            }
        }
    }
}
