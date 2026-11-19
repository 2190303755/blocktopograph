package com.mithrilmania.blocktopograph.ui.screen.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.settings.FlagStore
import com.mithrilmania.blocktopograph.settings.SourcedWorkspace
import com.mithrilmania.blocktopograph.settings.WorkspaceStore
import com.mithrilmania.blocktopograph.settings.asFlagState
import com.mithrilmania.blocktopograph.settings.setFlag
import com.mithrilmania.blocktopograph.storage.isLegacyExternalStorageAvailable
import com.mithrilmania.blocktopograph.storage.uuid
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.excludeTop
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.ui.rememberBridgedLazyListState
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.size
import com.mithrilmania.blocktopograph.util.todoI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun formatSize(size: Long?): String {
    return if (size === null) {
        stringResource(R.string.calculating_size)
    } else {
        ConvertUtil.formatSize(size)
    }
}

@Composable
private fun WorkspaceSummary(count: Int, totalSize: Long?) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.workspace_size_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        stringResource(R.string.workspace_count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(count.toString(), style = MaterialTheme.typography.headlineSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.workspace_total_size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatSize(totalSize),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceItem(
    entry: SourcedWorkspace,
    size: Long?,
    shapes: ListItemShapes,
    colors: ListItemColors,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
) {
    SegmentedListItem(
        shapes = shapes,
        colors = colors,
        modifier = modifier,
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.workspace_item_remove)
                )
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(
                        R.string.workspace_item_size,
                        formatSize(size)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    entry.first,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
    ) {
        Text(
            entry.second.uuid.toString(),
            maxLines = 1
        )
    }
}

@Composable
private fun WorkspaceControl(modifier: Modifier) {
    val shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
    if (isLegacyExternalStorageAvailable()) {
        val interactionSource = remember { MutableInteractionSource() }
        val coroutineScope = rememberCoroutineScope()
        val appContext = LocalContext.current.applicationContext
        val onCheckedChange: (Boolean) -> Unit = { enabled ->
            coroutineScope.launch {
                FlagStore.IS_WORKSPACE_ENABLED.setFlag(appContext, enabled)
            }
        }
        val isWorkspaceEnabled by FlagStore.IS_WORKSPACE_ENABLED.asFlagState(appContext, true)
        SegmentedListItem(
            checked = isWorkspaceEnabled,
            onCheckedChange = onCheckedChange,
            shapes = shapes,
            colors = colors,
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            interactionSource = interactionSource,
            leadingContent = { Icon(Icons.Filled.FolderCopy, null) },
            trailingContent = {
                Switch(
                    checked = isWorkspaceEnabled,
                    onCheckedChange = onCheckedChange,
                    interactionSource = interactionSource
                )
            },
            supportingContent = {
                Text(
                    if (isWorkspaceEnabled) stringResource(R.string.workspace_enabled_description)
                    else stringResource(R.string.workspace_disabled_description)
                )
            },
            content = { Text(stringResource(R.string.workspace_enable)) }
        )
    } else {
        SegmentedListItem(
            shapes = shapes,
            colors = colors,
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = { Icon(Icons.Filled.FolderCopy, null) },
            trailingContent = {
                Switch(
                    checked = true,
                    onCheckedChange = null,
                    enabled = false
                )
            },
            supportingContent = {
                Text(stringResource(R.string.workspace_enabled_description))
            },
            content = { Text(stringResource(R.string.workspace_forced)) }
        )
    }
}

@Composable
fun WorkspaceManagerScreen() {
    val appContext = LocalContext.current.applicationContext
    val entries = remember { WorkspaceStore.entries(appContext) }
    val sizes = remember { mutableStateMapOf<UUID, Long>() }
    var totalSize by remember { mutableStateOf<Long?>(null) }
    val items by entries.collectAsStateWithLifecycle(null)
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        entries.collectLatest { list ->
            totalSize = null
            sizes.clear()
            list.map {
                this.launch(Dispatchers.IO) {
                    sizes[it.second.uuid] = runInterruptible {
                        it.second.file.size
                    }
                }
            }.joinAll()
            totalSize = sizes.values.sum()
        }
    }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<SourcedWorkspace?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(AppNavKey.WorkspaceManager.title)) },
                navigationIcon = ::AppBarNavigationButton,
                actions = {
                    IconButton(Icons.Filled.Refresh) { ++reload }
                }
            )
        }
    ) { padding ->
        val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
        Box(
            Modifier.padding(padding.onlyTop()),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                state = rememberBridgedLazyListState(scope),
                contentPadding = padding.excludeTop() + PaddingValues(
                    start = 12.dp,
                    top = 10.dp,
                    bottom = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                item {
                    WorkspaceControl(
                        Modifier
                            .animateItem()
                            .padding(bottom = 10.dp)
                    )
                }
                item(key = "workspace_summary") {
                    WorkspaceSummary(items?.size ?: 0, totalSize)
                }
                val workspaces = items
                if (workspaces === null) {
                    item(key = "workspace_loading") {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("加载中…".todoI18n())
                        }
                    }
                } else if (workspaces.isEmpty()) {
                    item(key = "workspace_empty") {
                        Box(
                            Modifier
                                .fillParentMaxSize()
                                .animateItem(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.workspace_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        workspaces,
                        key = { _, entry -> entry.second.uuid }) { index, entry ->
                        WorkspaceItem(
                            entry = entry,
                            size = sizes[entry.second.uuid],
                            shapes = ListItemDefaults.segmentedShapes(
                                index = index,
                                count = workspaces.size
                            ),
                            colors = colors,
                            modifier = Modifier.animateItem(),
                            onDelete = { pendingDelete = entry }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.workspace_item_remove)) },
            text = {
                Text(stringResource(R.string.workspace_remove_message, entry.first))
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDelete = null
                }) { Text(stringResource(android.R.string.cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    appContext.externalCacheDir?.let { cache ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                WorkspaceStore.remove(appContext, entry.first)?.let {
                                    File(cache, it.uuid.toString()).deleteRecursively()
                                }
                            }
                        }
                    }
                }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }
}
