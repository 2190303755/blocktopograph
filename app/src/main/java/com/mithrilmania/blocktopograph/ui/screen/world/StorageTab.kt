package com.mithrilmania.blocktopograph.ui.screen.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.component.ExpanderIndicator
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.world.StandaloneKey
import com.mithrilmania.blocktopograph.world.hexedKey
import com.mithrilmania.blocktopograph.world.wrapAsFile
import kotlinx.coroutines.launch

@Composable
fun StorageTab(
    editor: WorldEditorModel,
    info: InitState.Succeed,
    padding: PaddingValues,
    onNavigate: (AppNavKey) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var playersExpanded by rememberSaveable { mutableStateOf(false) }
    var specialKeysExpanded by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (editor.players === null) {
            isRefreshing = true
            editor.loadPlayers(info.storage).join()
            isRefreshing = false
        }
    }
    val refresh: () -> Unit = {
        if (!isRefreshing) {
            isRefreshing = true
            coroutineScope.launch {
                editor.loadPlayers(info.storage).join()
                context.toast(R.string.general_done)
                isRefreshing = false
            }
        }
    }
    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
    val standaloneKeys = remember {
        listOf(
            StandaloneKey.BIOME_DATA,
            StandaloneKey.LEGACY_VILLAGE,
            StandaloneKey.PORTALS,
            StandaloneKey.AUTONOMOUS_ENTITIES
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        item {
            SegmentedListItem(
                onClick = {
                    onNavigate(AppNavKey.StorageExplorer(info.storage))
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp),
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                colors = colors,
                trailingContent = { Icon(Icons.Filled.Search, null) },
                content = { Text(stringResource(R.string.open_nbt_by_name)) }
            )
        }
        item {
            SegmentedListItem(
                onClick = {
                    onNavigate(
                        AppNavKey.NBTEditor(
                            editor.world.config to NBTImportConfigImpl(
                                NBTFormat.LITTLE_ENDIAN,
                                true
                            )
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = 8.dp),
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                colors = colors,
                trailingContent = { Icon(Icons.Filled.Settings, null) },
                content = { Text("世界配置".todoI18n()) }
            )
        }
        playerSection(
            editor = editor,
            isRefreshing = isRefreshing,
            onRefresh = refresh,
            expanded = playersExpanded,
            onExpandedChange = { playersExpanded = !playersExpanded },
            colors = colors
        )
        standaloneKeysSection(
            standaloneKeys = standaloneKeys,
            expanded = specialKeysExpanded,
            onExpandedChange = { specialKeysExpanded = !specialKeysExpanded },
            colors = colors,
            onEntryClick = { entry ->
                coroutineScope.launch {
                    val file = info.storage.wrapAsFile(entry)
                    if (file.isPresent(null)) {
                        onNavigate(AppNavKey.NBTEditor(file to file))
                    } else {
                        context.toast(R.string.cannot_find_db_entry_with_name)
                    }
                }
            }
        )
    }
}

private fun LazyListScope.playerSection(
    editor: WorldEditorModel,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    colors: ListItemColors
) {
    val players = editor.players
    val itemCount = (players?.size ?: 0) + 1
    item {
        SegmentedListItem(
            onClick = onExpandedChange,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 6.dp),
            shapes = ListItemDefaults.segmentedShapes(
                index = 0,
                count = if (expanded) itemCount else 1
            ),
            colors = colors,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RefreshIndicator(isRefreshing, onRefresh)
                    ExpanderIndicator(expanded)
                }
            },
            content = { Text(stringResource(R.string.locator_page_player)) }
        )
    }
    if (expanded) {
        when {
            players === null && !isRefreshing -> item {
                Text(
                    stringResource(R.string.general_failed),
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            players != null && players.isEmpty() -> item {
                Text(
                    stringResource(R.string.failed_find_player),
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            players != null -> itemsIndexed(
                players,
                key = { _, player -> player.dbName }) { index, player ->
                val context = LocalContext.current
                val position = player.position
                SegmentedListItem(
                    onClick = {
                        position?.let {
                            editor.moveCamera(it.x.toDouble(), it.z.toDouble())
                        }
                    },
                    enabled = position !== null,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .animateItem(),
                    shapes = ListItemDefaults.segmentedShapes(
                        index = index + 1,
                        count = itemCount
                    ),
                    colors = colors,
                    supportingContent = {
                        Text(player.getPositionDescription(context))
                    },
                    content = { Text(player.dbName) }
                )
            }
        }
    }
}

private fun LazyListScope.standaloneKeysSection(
    standaloneKeys: List<StandaloneKey>,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    colors: ListItemColors,
    onEntryClick: (StandaloneKey) -> Unit
) {
    val count = standaloneKeys.size + 1
    item {
        SegmentedListItem(
            onClick = onExpandedChange,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 6.dp),
            shapes = ListItemDefaults.segmentedShapes(
                index = 0,
                count = if (expanded) count else 1
            ),
            colors = colors,
            trailingContent = { ExpanderIndicator(expanded) },
            content = { Text("独立键名".todoI18n()) }
        )
    }
    if (expanded) {
        itemsIndexed(standaloneKeys, key = { _, key -> key.literal }) { index, entry ->
            SegmentedListItem(
                onClick = { onEntryClick(entry) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .animateItem(),
                shapes = ListItemDefaults.segmentedShapes(
                    index = index + 1,
                    count = count
                ),
                colors = colors,
                content = { Text(entry.literal) },
                supportingContent = {
                    Text("0x" + entry.hexedKey)
                }
            )
        }
    }
}
