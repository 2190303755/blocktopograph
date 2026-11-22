package com.mithrilmania.blocktopograph.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.LocalResourcePackViewModel
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.excludeTop
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.ui.rememberBridgedLazyListState
import kotlinx.coroutines.launch

@Composable
fun BlockTextureBindingScreen() = TextureBindingScreen(
    isBlock = true,
    titleRes = AppNavKey.BlockTextureBinding.title,
)

@Composable
fun ItemTextureBindingScreen() = TextureBindingScreen(
    isBlock = false,
    titleRes = AppNavKey.ItemTextureBinding.title,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextureBindingScreen(isBlock: Boolean, @StringRes titleRes: Int) {
    val scope = rememberCoroutineScope()
    val vm = LocalResourcePackViewModel.current
    val bindings by (if (isBlock) vm.blockBindings else vm.itemBindings)
        .collectAsStateWithLifecycle()
    val sortedBindings = remember(bindings) { bindings.toSortedMap() }
    var editing by remember { mutableStateOf<Pair<String, String>?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = ::AppBarNavigationButton
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, stringResource(R.string.binding_add))
            }
        }
    ) { padding ->
        val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
        LazyColumn(
            modifier = Modifier.padding(padding.onlyTop()),
            state = rememberBridgedLazyListState(),
            contentPadding = padding.excludeTop() + PaddingValues(
                start = 12.dp,
                top = 4.dp,
                bottom = 88.dp,
                end = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            if (sortedBindings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.binding_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            itemsIndexed(sortedBindings.toList(), key = { _, entry -> entry.first }) { index, entry ->
                val (identifier, textureName) = entry
                SegmentedListItem(
                    onClick = { editing = entry },
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = sortedBindings.size),
                    colors = colors,
                    modifier = Modifier.animateItem(),
                    trailingContent = {
                        IconButton(onClick = { pendingDelete = identifier }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.binding_remove)
                            )
                        }
                    },
                    content = { Text(identifier) },
                    supportingContent = { Text(textureName) }
                )
            }
        }
    }

    if (adding) {
        key("add") {
            BindingEditDialog(
                initial = null,
                onDismiss = { adding = false },
                onSave = { identifier, textureName ->
                    adding = false
                    scope.launch {
                        if (isBlock) vm.setBlockBinding(identifier, textureName)
                        else vm.setItemBinding(identifier, textureName)
                    }
                }
            )
        }
    }
    editing?.let { current ->
        key(current.first) {
            BindingEditDialog(
                initial = current,
                onDismiss = { editing = null },
                onSave = { identifier, textureName ->
                    editing = null
                    scope.launch {
                        if (isBlock) {
                            if (current.first != identifier) vm.removeBlockBinding(current.first)
                            vm.setBlockBinding(identifier, textureName)
                        } else {
                            if (current.first != identifier) vm.removeItemBinding(current.first)
                            vm.setItemBinding(identifier, textureName)
                        }
                    }
                }
            )
        }
    }
    pendingDelete?.let { identifier ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.binding_remove)) },
            text = { Text(stringResource(R.string.binding_remove_message, identifier)) },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch {
                        if (isBlock) vm.removeBlockBinding(identifier)
                        else vm.removeItemBinding(identifier)
                    }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun BindingEditDialog(
    initial: Pair<String, String>?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val identifier = rememberTextFieldState(initial?.first.orEmpty())
    val textureName = rememberTextFieldState(initial?.second.orEmpty())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.binding_add else R.string.binding_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    state = identifier,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text(stringResource(R.string.binding_identifier)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    state = textureName,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text(stringResource(R.string.binding_texture_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val id = identifier.text.toString().trim()
                val name = textureName.text.toString().trim()
                if (id.isNotEmpty() && name.isNotEmpty()) onSave(id, name)
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
