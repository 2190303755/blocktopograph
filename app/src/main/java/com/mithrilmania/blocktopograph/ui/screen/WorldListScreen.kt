package com.mithrilmania.blocktopograph.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.isShizukuAvailable
import com.mithrilmania.blocktopograph.settings.FlagStore
import com.mithrilmania.blocktopograph.settings.ScanningStore
import com.mithrilmania.blocktopograph.settings.getFlag
import com.mithrilmania.blocktopograph.storage.awaitFileService
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.BlankNBTEditor
import com.mithrilmania.blocktopograph.ui.ScrollableStateBridge
import com.mithrilmania.blocktopograph.ui.WorldDetailDialog
import com.mithrilmania.blocktopograph.ui.WorldLoaderDialog
import com.mithrilmania.blocktopograph.ui.component.HiddenOrExpanded
import com.mithrilmania.blocktopograph.ui.component.PastableDialog
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.WorldItem
import com.mithrilmania.blocktopograph.ui.component.clickableItem
import com.mithrilmania.blocktopograph.ui.component.excludeTop
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.util.collectText
import com.mithrilmania.blocktopograph.util.rpc
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.world.access.WorldAccess
import com.mithrilmania.blocktopograph.world.buildWorld
import com.mithrilmania.blocktopograph.world.defaultWorldPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldListScreen(onTop: State<Boolean>, onNavigate: (AppNavKey) -> Unit) {
    val viewModel = viewModel<WorldListModel>()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val scans = ScanningStore.read(context)
        scans.uris.forEach { viewModel.unscanned.send("" to it) }
        if (scans.paths.isNotEmpty()
            && FlagStore.IS_SHIZUKU_ALLOWED.getFlag(context) ?: false
            && isShizukuAvailable()
        ) withContext(Dispatchers.IO) {
            awaitFileService()?.let { service ->
                scans.paths.forEach { path ->
                    service.rpc {
                        it.loadWorlds(path, viewModel.callback)
                    }
                }
            }
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(maxFontSize = LocalTextStyle.current.fontSize)
                    )
                },
                actions = {
                    val resources = LocalResources.current
                    AppBarRow(maxItemCount = 3) {
                        clickableItem(
                            Icons.Filled.DriveFolderUpload,
                            resources.getString(R.string.open)
                        ) {
                            if (isShizukuAvailable()) {
                                coroutineScope.launch {
                                    if (FlagStore.IS_SHIZUKU_ALLOWED.getFlag(context) ?: false) {
                                        if (Blocktopograph.fileService.service === null) {
                                            Toast.makeText(
                                                context,
                                                "Connecting".todoI18n(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            if (withContext(Dispatchers.IO) { awaitFileService() } === null) {
                                                Toast.makeText(
                                                    context,
                                                    "Failed".todoI18n(),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                viewModel.openViaShizuku = true
                                            }
                                        } else {
                                            viewModel.openViaShizuku = true
                                        }
                                    } else {
                                        onNavigate(AppNavKey.Settings)
                                    }
                                }
                            } else {
                                onNavigate(AppNavKey.Settings)
                            }
                        }
                        clickableItem(
                            Icons.Filled.Edit,
                            resources.getString(R.string.nbt_editor)
                        ) {
                            onNavigate(BlankNBTEditor)
                        }
                        clickableItem(
                            Icons.Filled.Add,
                            resources.getString(R.string.action_create)
                        ) {
                            onNavigate(AppNavKey.CreateWorld)
                        }
                        clickableItem(
                            Icons.Filled.Info,
                            resources.getString(R.string.action_about)
                        ) {
                            viewModel.showAbout = true
                        }
                        clickableItem(
                            Icons.Filled.Settings,
                            resources.getString(R.string.action_settings)
                        ) {
                            onNavigate(AppNavKey.Settings)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(viewModel.snackbar) },
        floatingActionButton = {
            TooltipBox(stringResource(R.string.open)) { tooltip ->
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { folder ->
                    if (folder == null) return@rememberLauncherForActivityResult
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        viewModel.unscanned.send("" to folder)
                    }
                }
                FloatingActionButton(onClick = { picker.launch(null) }) {
                    Icon(Icons.Filled.AddLocationAlt, tooltip)
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding.onlyTop())) {
            val gridState = rememberLazyGridState()
            AndroidView(::ScrollableStateBridge) {
                it.bind(gridState, coroutineScope)
            }
            AnimatedVisibility(
                visible = viewModel.loading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(1.0F)
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            val spacing = Arrangement.spacedBy(12.dp)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = padding.excludeTop() + PaddingValues(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),
                verticalArrangement = spacing,
                horizontalArrangement = spacing
            ) {
                items(items = viewModel.worlds, key = { it.location.location }) {
                    WorldItem(it, Modifier.animateItem()) {
                        viewModel.selected = it
                    }
                }
            }
            val loading = remember { mutableStateOf<WorldAccess?>(null) }
            val sheetState = rememberBottomSheetState(SheetValue.Hidden, HiddenOrExpanded)
            viewModel.selected?.let { selected ->
                if (onTop.value || sheetState.isVisible) {
                    val context = LocalContext.current
                    WorldDetailDialog(
                        detail = selected,
                        state = sheetState,
                        onNavigate = { key ->
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onNavigate(key)
                            }
                        },
                        onDismiss = { viewModel.selected = null },
                    ) {
                        loading.value = selected.buildWorld(context)
                    }
                }
            }
            loading.value?.let { access ->
                WorldLoaderDialog(
                    access = access,
                    onCancel = { loading.value = null }
                ) { deferred ->
                    coroutineScope.launch {
                        sheetState.hide()
                        val world = try {
                            deferred.await()
                        } catch (_: CancellationException) {
                            return@launch
                        } catch (_: Exception) {
                            sheetState.show()
                            return@launch
                        }
                        onNavigate(AppNavKey.WorldEditor(world))
                        loading.value = null
                    }
                }
            }
        }
    }
    if (viewModel.showAbout) {
        val primary = MaterialTheme.colorScheme.primary
        val linkStyles = remember(primary) {
            TextLinkStyles(
                style = SpanStyle(
                    color = primary,
                    textDecoration = TextDecoration.Underline
                )
            )
        }
        AlertDialog(
            onDismissRequest = { viewModel.showAbout = false },
            title = { Text(stringResource(R.string.action_about)) },
            text = {
                val resources = LocalResources.current
                Text(
                    text = AnnotatedString.fromHtml(
                        htmlString = resources.getText(R.string.app_about)
                            .toSpanned()
                            .toHtml(),
                        linkStyles = linkStyles
                    ),
                    modifier = Modifier
                        .heightIn(max = LocalDensity.current.run {
                            LocalTextStyle.current.lineHeight.toDp() * 20
                        })
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(stringResource(android.R.string.ok)) {
                    viewModel.showAbout = false
                }
            }
        )
    }
    if (viewModel.openViaShizuku) {
        val input = rememberTextFieldState(defaultWorldPath())
        PastableDialog(
            title = "加载世界".todoI18n(),
            onCancel = { viewModel.openViaShizuku = false },
            onPaste = {
                val text = it?.collectText()
                if (text === null) {
                    context.toast(R.string.toast_empty_clipboard)
                } else {
                    input.setTextAndPlaceCursorAtEnd(text)
                }
            },
            onConfirm = {
                viewModel.openViaShizuku = false
                coroutineScope.launch(Dispatchers.IO) {
                    val service = awaitFileService()
                    if (service === null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "File service timeout",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }
                    val path = input.text.toString()
                    if (path.isBlank() || service.rpc {
                            it.loadWorlds(
                                path,
                                viewModel.callback
                            )
                        } != true) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "invalid path".todoI18n(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        ) {
            OutlinedTextField(input, shape = OutlinedTextFieldDefaults.roundedShape)
        }
    }
}