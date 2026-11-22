package com.mithrilmania.blocktopograph.ui.screen.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Browser
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.texture.ResourcePack
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.LocalResourcePackViewModel
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.excludeTop
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.ui.rememberBridgedLazyListState
import com.mithrilmania.blocktopograph.util.toast
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val OFFICIAL_PACKS_URL = "https://github.com/Mojang/bedrock-samples/releases"
private const val HEADER_COUNT = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcePackManagerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val vm = LocalResourcePackViewModel.current
    val packs by vm.packs.collectAsStateWithLifecycle()
    var ordered by remember { mutableStateOf(packs) }
    var importing by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ResourcePack?>(null) }

    LaunchedEffect(packs) { ordered = packs }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val success = runCatching { vm.import(uri) }.isSuccess
            importing = false
            context.toast(
                if (success) R.string.resource_pack_imported else R.string.resource_pack_import_failed
            )
        }
    }

    fun openOfficialPacks() {
        val intent = Intent(Intent.ACTION_VIEW, OFFICIAL_PACKS_URL.toUri())
            .putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.toast(R.string.general_failed)
        }
    }

    fun toggle(pack: ResourcePack, enabled: Boolean) {
        scope.launch { vm.setEnabled(pack.id, enabled) }
    }

    val listState = rememberBridgedLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        if (from.index < HEADER_COUNT || to.index < HEADER_COUNT) {
            return@rememberReorderableLazyListState
        }
        ordered = ordered.toMutableList().also { list ->
            list.add((to.index - HEADER_COUNT).coerceIn(0, list.lastIndex), list.removeAt(from.index - HEADER_COUNT))
        }
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(AppNavKey.ResourcePackManager.title)) },
                navigationIcon = ::AppBarNavigationButton
            )
        }
    ) { padding ->
        val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
        LazyColumn(
            modifier = Modifier.padding(padding.onlyTop()),
            state = listState,
            contentPadding = padding.excludeTop() + PaddingValues(
                start = 12.dp,
                top = 4.dp,
                bottom = 12.dp,
                end = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            item {
                SegmentedListItem(
                    onClick = { if (!importing) picker.launch(arrayOf("*/*")) },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = HEADER_COUNT),
                    colors = colors,
                    leadingContent = { Icon(Icons.Filled.UploadFile, null) },
                    trailingContent = {
                        if (importing) CircularProgressIndicator()
                    },
                    content = { Text(stringResource(R.string.resource_pack_import)) },
                    supportingContent = { Text(stringResource(R.string.resource_pack_import_hint)) }
                )
            }
            item {
                SegmentedListItem(
                    onClick = ::openOfficialPacks,
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = HEADER_COUNT),
                    colors = colors,
                    leadingContent = { Icon(Icons.Filled.Download, null) },
                    content = { Text(stringResource(R.string.resource_pack_download)) },
                    supportingContent = { Text(stringResource(R.string.resource_pack_download_hint)) }
                )
            }
            if (ordered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.resource_pack_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            itemsIndexed(ordered, key = { _, pack -> pack.id }) { index, pack ->
                ReorderableItem(reorderableState, pack.id) { _ ->
                    // draggableHandle is a ReorderableItem extension, so it only exists in here
                    val handle = Modifier.draggableHandle(
                        onDragStarted = {
                            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        },
                        onDragStopped = {
                            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            scope.launch { vm.reorder(ordered.map { it.id }) }
                        }
                    )
                    SegmentedListItem(
                        checked = pack.enabled,
                        onCheckedChange = { toggle(pack, it) },
                        shapes = ListItemDefaults.segmentedShapes(
                            index = index + HEADER_COUNT,
                            count = ordered.size + HEADER_COUNT
                        ),
                        colors = colors,
                        modifier = Modifier.animateItem(),
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.resource_pack_reorder),
                                modifier = handle
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = pack.enabled,
                                    onCheckedChange = { toggle(pack, it) }
                                )
                                IconButton(onClick = { pendingDelete = pack }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.resource_pack_remove)
                                    )
                                }
                            }
                        },
                        content = { Text(pack.id) },
                        supportingContent = { Text(pack.zipPath.substringAfterLast('/')) }
                    )
                }
            }
        }
    }

    pendingDelete?.let { pack ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.resource_pack_remove)) },
            text = { Text(stringResource(R.string.resource_pack_remove_message, pack.id)) },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { vm.remove(pack.id) }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}
