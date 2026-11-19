package com.mithrilmania.blocktopograph.ui.screen.settings

import android.content.ClipData
import android.content.Intent
import android.content.UriPermission
import android.provider.DocumentsContract.isDocumentUri
import android.provider.DocumentsContract.isTreeUri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.settings.ScanningStore
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.excludeTop
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.util.runSuppressing
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.todoI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private enum class UriTypeFilter(@StringRes val label: Int) {
    ANY(R.string.saf_filter_type_any),
    TREE(R.string.saf_filter_type_tree),
    DOCUMENT(R.string.saf_filter_type_document)
}

private enum class UriAccessFilter(@StringRes val label: Int) {
    ANY(R.string.saf_filter_access_any),
    READ_WRITE(R.string.saf_filter_access_read_write),
    READ_ONLY(R.string.saf_filter_access_read_only),
    WRITE_ONLY(R.string.saf_filter_access_write_only)
}

@Composable
private fun UriPermissionItem(
    permission: UriPermission,
    shapes: ListItemShapes,
    modifier: Modifier,
    onRevoke: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val isTree = isTreeUri(permission.uri)
    SegmentedListItem(
        selected = menuExpanded,
        onClick = { menuExpanded = true },
        modifier = modifier,
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Icon(
                if (isTree) Icons.Filled.Folder else Icons.Filled.Description,
                contentDescription = null
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(permission.uri.toString())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Badge((if (permission.isWritePermission) "可读写" else "只读").todoI18n())
                }
            }
        }
    ) {
        Text(
            text = permission.uri.lastPathSegment ?: permission.uri.toString(),
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenuPopup(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
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
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText("URI", permission.uri.toString())
                                .toClipEntry()
                        )
                        context.toast(R.string.toast_copy_success)
                    }
                    menuExpanded = false
                }
            }
            Spacer(Modifier.size(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(MenuDefaults.groupShape(1, 2)) {
                val color = MaterialTheme.colorScheme.error
                CompositionLocalProvider(LocalContentColor provides color) {
                    DropdownMenuItem(
                        icon = Icons.Filled.Delete,
                        label = stringResource(R.string.saf_revoke),
                        shape = MenuDefaults.trailingItemShape,
                        colors = MenuDefaults.itemColors(
                            textColor = color,
                            leadingIconColor = color
                        )
                    ) {
                        onRevoke()
                        menuExpanded = false
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAFManagerScreen() {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var pendingRevoke by remember { mutableStateOf<UriPermission?>(null) }
    val request = remember { MutableStateFlow(1) }
    val latest = request.collectAsStateWithLifecycle(1)
    var typeFilter by rememberSaveable { mutableStateOf(UriTypeFilter.ANY) }
    var accessFilter by rememberSaveable { mutableStateOf(UriAccessFilter.ANY) }
    val permissions = remember {
        request.map {
            it to appContext.contentResolver.persistedUriPermissions
        }.flowOn(Dispatchers.IO)
    }.collectAsStateWithLifecycle(0 to emptyList())
    val refreshing = remember {
        derivedStateOf { permissions.value.first != latest.value }
    }
    val filtered = remember {
        derivedStateOf {
            permissions.value.second.filter {
                when (accessFilter) {
                    UriAccessFilter.ANY -> true
                    UriAccessFilter.READ_WRITE -> it.isReadPermission && it.isWritePermission
                    UriAccessFilter.READ_ONLY -> it.isReadPermission && !it.isWritePermission
                    UriAccessFilter.WRITE_ONLY -> !it.isReadPermission && it.isWritePermission
                } && when (typeFilter) {
                    UriTypeFilter.ANY -> true
                    UriTypeFilter.TREE -> isTreeUri(it.uri)
                    UriTypeFilter.DOCUMENT -> isDocumentUri(appContext, it.uri)
                }
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(AppNavKey.SAFManager.title)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = ::AppBarNavigationButton,
                actions = {
                    var expended by rememberSaveable { mutableStateOf(false) }
                    TooltipBox(stringResource(R.string.saf_filter)) { tooltip ->
                        IconToggleButton(
                            checked = expended,
                            onCheckedChange = { expended = it }
                        ) {
                            Icon(Icons.Filled.FilterList, tooltip)
                        }
                    }
                    DropdownMenuPopup(
                        expanded = expended,
                        onDismissRequest = { expended = false },
                        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                            MenuAnchorPosition.Below
                        )
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(0, 2),
                            modifier = Modifier.selectableGroup()
                        ) {
                            MenuDefaults.DropdownMenuGroupLabel { Text(stringResource(R.string.saf_filter_type)) }
                            HorizontalDivider(Modifier.padding(MenuDefaults.HorizontalDividerPadding))
                            UriTypeFilter.entries.forEachIndexed { index, filter ->
                                SelectableDropdownMenuItem(
                                    selected = typeFilter == filter,
                                    onClick = { typeFilter = filter },
                                    text = { Text(stringResource(filter.label)) },
                                    shapes = MenuDefaults.itemShape(
                                        index,
                                        UriTypeFilter.entries.size
                                    ),
                                    selectedLeadingIcon = {
                                        Icon(
                                            Icons.Filled.Check,
                                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                            contentDescription = null,
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.size(MenuDefaults.GroupSpacing))
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(1, 2),
                            modifier = Modifier.selectableGroup()
                        ) {
                            MenuDefaults.DropdownMenuGroupLabel { Text(stringResource(R.string.saf_filter_permission)) }
                            HorizontalDivider(Modifier.padding(MenuDefaults.HorizontalDividerPadding))
                            UriAccessFilter.entries.forEachIndexed { index, filter ->
                                SelectableDropdownMenuItem(
                                    selected = accessFilter == filter,
                                    onClick = { accessFilter = filter },
                                    text = { Text(stringResource(filter.label)) },
                                    shapes = MenuDefaults.itemShape(
                                        index,
                                        UriAccessFilter.entries.size
                                    ),
                                    selectedLeadingIcon = {
                                        Icon(
                                            Icons.Filled.Check,
                                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                            contentDescription = null,
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing.value,
            onRefresh = { ++request.value },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.onlyTop())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding.excludeTop() + PaddingValues(
                    start = 12.dp,
                    top = 4.dp,
                    bottom = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                if (filtered.value.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .animateItem(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(
                                    if (permissions.value.second.isEmpty()) R.string.saf_empty
                                    else R.string.saf_empty_filtered
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                itemsIndexed(
                    items = filtered.value,
                    key = { _, it -> it }
                ) { index, permission ->
                    UriPermissionItem(
                        permission = permission,
                        shapes = ListItemDefaults.segmentedShapes(
                            index = index,
                            count = filtered.value.size
                        ),
                        modifier = Modifier.animateItem(),
                        onRevoke = { pendingRevoke = permission }
                    )
                }
            }
        }
    }

    pendingRevoke?.let { permission ->
        val uri = permission.uri
        val onDismissRequest = { pendingRevoke = null }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.saf_revoke_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.saf_revoke_message))
                    Text(uri.toString())
                }
            },
            dismissButton = {
                TextButton(stringResource(android.R.string.cancel), onClick = onDismissRequest)
            },
            confirmButton = {
                TextButton(stringResource(android.R.string.ok)) {
                    pendingRevoke = null
                    scope.launch(Dispatchers.IO) {
                        val flags =
                            (if (permission.isReadPermission)
                                Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                                    (if (permission.isWritePermission)
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
                        runSuppressing {
                            appContext.contentResolver.releasePersistableUriPermission(uri, flags)
                        }
                        // The scan location is no longer accessible without the permission.
                        ScanningStore.remove(appContext, uri)
                        ++request.value
                    }
                }
            }
        )
    }
}
