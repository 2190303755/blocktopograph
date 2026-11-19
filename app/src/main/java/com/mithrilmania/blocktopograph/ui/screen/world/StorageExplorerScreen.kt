package com.mithrilmania.blocktopograph.ui.screen.world

import android.content.ClipData
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExpandedDockedSearchBarWithGap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarWithGapState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.world.collectMatches
import com.mithrilmania.blocktopograph.map.dummyJob
import com.mithrilmania.blocktopograph.storage.VirtualFile
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.awaitSnackbar
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.LEVEL_DB_TAG
import com.mithrilmania.blocktopograph.util.VIEW_DOCUMENT_FLAG
import com.mithrilmania.blocktopograph.util.todoI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.iq80.leveldb.DB

class LevelDBKey(@JvmField val bytes: ByteArray) {
    @JvmField
    val hexed: String = "0x" + this.bytes.toHexString()

    @JvmField
    val plain: String = this.bytes.toString(Charsets.UTF_8)

    @JvmField
    val hasISOControl: Boolean = this.plain.any { it.isISOControl() }
}

fun DB.wrapAsFile(key: LevelDBKey): VirtualFile =
    VirtualFile(this, if (key.hasISOControl) key.hexed else key.plain, key.bytes)


class StorageExplorerModel(
    @JvmField val storage: DB
) : ViewModel() {
    var exporting: LevelDBKey? by mutableStateOf(null)
    var isHexed by mutableStateOf(false)
    val input: TextFieldState = TextFieldState()
    val entries: MutableList<LevelDBKey> = mutableStateListOf()
    val snackbar: SnackbarHostState = SnackbarHostState()
    var searching by mutableStateOf(false)
        private set
    private var searchJob: Job = dummyJob()

    fun resolvePattern(isHexed: Boolean = this.isHexed): ByteArray {
        val input = this.input.text.trim().toString()
        if (isHexed) {
            try {
                return input.hexToByteArray()
            } catch (_: IllegalArgumentException) {
            }
        }
        return input.toByteArray(Charsets.UTF_8)
    }

    /**
     * Searches DB keys matching the current search-field content. Plain input is matched as
     * UTF-8 bytes; hex input must contain an even number of hex digits.
     */
    fun search() {
        val pattern = this.resolvePattern()
        searchJob.cancel()
        entries.clear()
        searching = true
        searchJob = viewModelScope.launch {
            storage.collectMatches(pattern, entries)
            searching = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageExplorerScreen(
    storage: DB,
    onNavigate: (AppNavKey) -> Unit
) {
    val model = viewModel { StorageExplorerModel(storage) }
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val creator = rememberLauncherForActivityResult(FileCreator) callback@{ uri ->
        if (uri === null) return@callback
        val exporting = model.exporting ?: return@callback
        scope.launch(Dispatchers.IO) {
            val bytes = storage[exporting.bytes] ?: return@launch
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            } catch (e: Throwable) {
                Log.e(LEVEL_DB_TAG, "Failed to export key ${exporting.bytes}", e)
                error = e.stackTraceToString()
                return@launch
            }
            model.snackbar.awaitSnackbar(
                message = resources.getString(R.string.world_test_export_done),
                actionLabel = resources.getString(R.string.world_test_open_file),
                duration = SnackbarDuration.Long
            ) {
                withContext(Dispatchers.Main) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setFlags(VIEW_DOCUMENT_FLAG)
                            .setDataAndType(uri, MIME_TYPE_DEFAULT)
                    )
                }
            }
        }
    }
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val searchBarState = rememberSearchBarWithGapState()
    val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors()
    val inputField =
        @Composable {
            SearchBarDefaults.InputField(
                textFieldState = model.input,
                searchBarState = searchBarState,
                colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
                onSearch = {
                    model.search()
                    scope.launch { searchBarState.animateToCollapsed() }
                },
                placeholder = {
                    Text(modifier = Modifier.clearAndSetSemantics {}, text = "Search".todoI18n())
                },
                leadingIcon = {
                    if (searchBarState.currentValue == SearchBarValue.Expanded) {
                        TooltipBox("Back".todoI18n()) {
                            IconButton(Icons.AutoMirrored.Filled.ArrowBack) {
                                scope.launch { searchBarState.animateToCollapsed() }
                            }
                        }
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                },
                prefix = {
                    if (model.isHexed) {
                        Text("0x")
                    }
                },
                inputTransformation = {
                    if (model.isHexed) {
                        if (this.asCharSequence()
                                .startsWith("0x", ignoreCase = true)
                        ) {
                            delete(0, 2)
                        }
                        if (this.asCharSequence().any {
                                Character.digit(it.code, 16) < 0
                            }
                        ) {
                            revertAllChanges()
                        }
                    }
                }
            )
        }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppBarWithSearch(
                scrollBehavior = scrollBehavior,
                state = searchBarState,
                colors = appBarWithSearchColors,
                inputField = inputField,
                windowInsets = WindowInsets(),
                contentPadding = SearchBarDefaults.windowInsets.asPaddingValues(),
                navigationIcon = {
                    AnimatedVisibility(
                        visible = searchBarState.targetValue == SearchBarValue.Collapsed,
                        enter = slideIn(
                            animationSpec = motionScheme.fastSpatialSpec(),
                            initialOffset = { IntOffset(-it.width, 0) },
                        ),
                        exit = slideOut(
                            animationSpec = tween(durationMillis = 150, delayMillis = 0),
                            targetOffset = { IntOffset(-it.width, 0) },
                        )
                    ) {
                        AppBarNavigationButton()
                    }
                }
            )
            ExpandedDockedSearchBarWithGap(
                state = searchBarState,
                inputField = inputField,
                dropdownShape = MaterialTheme.shapes.large,
                colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                ListItem(
                    checked = model.isHexed,
                    onCheckedChange = {
                        val wasHexed = model.isHexed
                        if (wasHexed != it) {
                            val bytes = model.resolvePattern(wasHexed)
                            model.input.setTextAndPlaceCursorAtEnd(
                                if (it) bytes.toHexString() else bytes.toString(Charsets.UTF_8)
                            )
                        }
                        model.isHexed = it
                    },
                    leadingContent = {
                        Icon(imageVector = Icons.Filled.Code, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = model.isHexed,
                            onCheckedChange = null,
                            interactionSource = interactionSource
                        )
                    },
                    interactionSource = interactionSource
                ) {
                    Text("十六进制输入".todoI18n())
                }
            }
        },
        snackbarHost = { SnackbarHost(model.snackbar) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            items(model.entries, key = { it.hexed }) { entry ->
                ListItem(
                    modifier = Modifier.animateItem(),
                    overlineContent = { Text(entry.hexed) },
                    trailingContent = {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.wrapContentSize()) {
                            SplitButtonLayout(
                                leadingButton = {
                                    SplitButtonDefaults.TonalLeadingButton(onClick = {
                                        val file = storage.wrapAsFile(entry)
                                        onNavigate(AppNavKey.NBTEditor(file to file))
                                    }) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            modifier = Modifier.size(
                                                SplitButtonDefaults.LeadingIconSize
                                            ),
                                            contentDescription = "Localized description",
                                        )
                                    }
                                },
                                trailingButton = {
                                    TooltipBox("Toggle Button") { tooltip ->
                                        SplitButtonDefaults.TonalTrailingButton(
                                            checked = expanded,
                                            onCheckedChange = { expanded = it },
                                            modifier = Modifier.semantics {
                                                stateDescription =
                                                    if (expanded) "Expanded"
                                                    else "Collapsed"
                                                contentDescription = tooltip
                                            },
                                        ) {
                                            val rotation: Float by animateFloatAsState(
                                                targetValue = if (expanded) 180f else 0f,
                                                label = "Trailing Icon Rotation",
                                            )
                                            Icon(
                                                Icons.Filled.KeyboardArrowDown,
                                                modifier = Modifier
                                                    .size(
                                                        SplitButtonDefaults
                                                            .TrailingIconSize
                                                    )
                                                    .graphicsLayer {
                                                        this.rotationZ = rotation
                                                    },
                                                contentDescription =
                                                    "Localized description",
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.wrapContentSize()
                            )
                            DropdownMenuPopup(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 2)) {
                                    val clipboard = LocalClipboard.current
                                    DropdownMenuItem(
                                        text = { Text("复制键名（文本）".todoI18n()) },
                                        onClick = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    ClipData.newPlainText("Key", entry.plain)
                                                        .toClipEntry()
                                                )
                                            }
                                        },
                                        shape = MenuDefaults.leadingItemShape,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.ContentCopy,
                                                contentDescription = null
                                            )
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("复制键名（十六进制）".todoI18n()) },
                                        onClick = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    ClipData.newPlainText("Key", entry.hexed)
                                                        .toClipEntry()
                                                )
                                            }
                                        },
                                        shape = MenuDefaults.middleItemShape,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.ContentCopy,
                                                contentDescription = null
                                            )
                                        },
                                    )
                                }
                                Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                                DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 2)) {
                                    DropdownMenuItem(
                                        text = { Text("导出".todoI18n()) },
                                        onClick = {
                                            model.exporting = entry
                                            creator.launch(
                                                FileCreator.Options(
                                                    mime = MIME_TYPE_DEFAULT,
                                                    name = if (entry.hasISOControl) entry.hexed else entry.plain
                                                )
                                            )
                                        },
                                        shape = MenuDefaults.trailingItemShape,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Output,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text(entry.plain)
                }
            }
        }
    }
    error?.let {
        val onDismissRequest = { error = null }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(stringResource(android.R.string.ok), onClick = onDismissRequest)
            },
            title = {
                Text("错误".todoI18n())
            },
            text = {
                Text(it, Modifier.verticalScroll(rememberScrollState()))
            }
        )
    }
}
