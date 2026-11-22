package com.mithrilmania.blocktopograph.ui.screen

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.world.FlatLayer
import com.mithrilmania.blocktopograph.editor.world.copy
import com.mithrilmania.blocktopograph.editor.world.toJson
import com.mithrilmania.blocktopograph.map.Biome
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readBinaryTag
import com.mithrilmania.blocktopograph.nbt.io.writeNBTWithHeader
import com.mithrilmania.blocktopograph.ui.BlockStatePreview
import com.mithrilmania.blocktopograph.ui.PickBlockDialog
import com.mithrilmania.blocktopograph.ui.rememberBlockTextureRequest
import com.mithrilmania.blocktopograph.ui.component.AnimatedBottomSheetDialog
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuField
import com.mithrilmania.blocktopograph.ui.component.HiddenOrExpanded
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.InfoBar
import com.mithrilmania.blocktopograph.ui.component.SwipeableSnackbar
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.applyInfoBarPadding
import com.mithrilmania.blocktopograph.ui.component.awaitSnackbar
import com.mithrilmania.blocktopograph.ui.component.onlyBottom
import com.mithrilmania.blocktopograph.ui.component.onlyHorizontal
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.ui.rememberBridgedLazyListState
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.util.tryOrNull
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.KEY_FLAT_WORLD_LAYERS
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_TIME
import com.mithrilmania.blocktopograph.world.KEY_LEVEL_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorldScreen(onCreate: (Uri) -> Unit) {
    val viewModel = viewModel<CreateWorldModel>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(stringResource(R.string.create_world_title))
                },
                navigationIcon = ::AppBarNavigationButton,
                actions = {
                    val tooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = {
                            PlainTooltip { Text(stringResource(R.string.edit_flat_help)) }
                        },
                        state = tooltipState
                    ) {
                        val scope = rememberCoroutineScope()
                        IconButton(
                            Icons.AutoMirrored.Filled.Help,
                            stringResource(R.string.action_help)
                        ) {
                            scope.launch { tooltipState.show(MutatePriority.UserInput) }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(viewModel.snackbar) {
                SwipeableSnackbar(it)
            }
        },
        floatingActionButton = {
            TooltipBox(stringResource(R.string.create)) { tooltip ->
                val appContext = LocalContext.current.applicationContext
                val resources = LocalResources.current
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { folderTree ->
                    if (folderTree === null) return@rememberLauncherForActivityResult
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val folderDocument = DocumentsContract.buildDocumentUriUsingTree(
                            folderTree,
                            DocumentsContract.getTreeDocumentId(folderTree)
                        ) ?: return@launch
                        val config = DocumentsContract.createDocument(
                            appContext.contentResolver,
                            folderDocument,
                            MIME_TYPE_DEFAULT,
                            FILE_LEVEL_DAT
                        ) ?: return@launch
                        val data = tryOrNull {
                            BedrockNBTInput(
                                appContext.assets.open("dats/1_2_13.dat").buffered()
                            ).use {
                                it.skipBytes(8)
                                it.readBinaryTag() as? CompoundTag
                            }
                        } ?: return@launch
                        data[KEY_LEVEL_NAME] = StringTag(
                            viewModel.name.text.ifBlank {
                                resources.getString(R.string.world_default_name)
                            }.toString()
                        )
                        data[KEY_LAST_PLAYED_TIME] = LongTag(
                            System.currentTimeMillis() / 1000
                        )
                        var layers: MutableList<FlatLayer> = viewModel.layers
                        if (layers.size < 3) {
                            layers = layers.toMutableList()
                            repeat(3 - layers.size) {
                                layers.add(FlatLayer(height = 0))
                            }
                        }
                        layers.toJson(viewModel.biome)?.let {
                            data[KEY_FLAT_WORLD_LAYERS] = StringTag(it)
                        }
                        (appContext.contentResolver.openOutputStream(config)
                            ?: return@launch)
                            .writeNBTWithHeader(4, "Blocktopograph", data)
                        withContext(Dispatchers.Main) {
                            appContext.toast(R.string.general_done)
                            onCreate(folderTree)
                        }
                    }
                }
                FloatingActionButton(onClick = { picker.launch(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, tooltip)
                }
            }
        }
    ) { padding ->
        val scope = rememberCoroutineScope()
        val listState = rememberBridgedLazyListState(scope)
        val inset = padding.onlyHorizontal()
        val hapticFeedback = LocalHapticFeedback.current
        val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
            viewModel.layers.apply {
                add(to.index - 2, removeAt(from.index - 2))
            }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
        LazyColumn(
            contentPadding = padding.onlyBottom(),
            state = listState,
            modifier = Modifier
                .padding(padding.onlyTop())
                .fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier.padding(
                        inset + PaddingValues(horizontal = 16.dp)
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        state = viewModel.name,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OutlinedTextFieldDefaults.roundedShape,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        label = { Text(stringResource(R.string.create_world_name)) },
                        labelPosition = TextFieldLabelPosition.Cutout(isAlwaysMinimized = true),
                        placeholder = { Text(stringResource(R.string.world_default_name)) },
                    )
                    DropdownMenuField(
                        options = listOf(Unit),
                        label = stringResource(R.string.create_world_version),
                        selected = Unit,
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { }
                    ) {
                        stringResource(R.string.create_world_version_aqua)
                    }
                    DropdownMenuField(
                        options = Biome.entries,
                        label = stringResource(R.string.biomes),
                        selected = viewModel.biome,
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { viewModel.biome = it }
                    ) { "${it.name} (${it.id})" }
                }
            }
            stickyHeader {
                Box {
                    val background = MaterialTheme.colorScheme.background
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(16.dp)
                            .offset(y = 16.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        background.copy(alpha = 1.0F),
                                        background.copy(alpha = 0.8F),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .background(background)
                            .padding(
                                inset + PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            )
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.create_world_layers),
                            style = MaterialTheme.typography.labelMedium
                        )
                        FilledTonalButton(
                            onClick = {
                                viewModel.snackbar.currentSnackbarData?.dismiss()
                                viewModel.layers.add(0, FlatLayer())
                            },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = ButtonDefaults.contentPaddingFor(
                                ButtonDefaults.MinHeight,
                                hasStartIcon = true
                            )
                        ) {
                            val label = "添加层".todoI18n()
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = label,
                                modifier = Modifier.size(
                                    ButtonDefaults.iconSizeFor(ButtonDefaults.MinHeight)
                                )
                            )
                            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                            Text(label)
                        }
                    }
                }
            }
            items(
                count = viewModel.layers.size,
                key = { viewModel.layers[it].uid }
            ) { index ->
                val layer = viewModel.layers[index]
                ReorderableItem(reorderableState, layer.uid) { _ ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Remove item".todoI18n(),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .wrapContentSize(Alignment.CenterEnd)
                                    .padding(end = 16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onDismiss = { direction ->
                            if (direction == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.snackbar.currentSnackbarData?.dismiss()
                                viewModel.layers.removeAt(index)
                                scope.launch {
                                    viewModel.snackbar.awaitSnackbar(
                                        message = "已删除".todoI18n(),
                                        actionLabel = "撤销".todoI18n(),
                                        duration = SnackbarDuration.Long
                                    ) {
                                        viewModel.layers.add(index, layer.copy())
                                    }
                                }
                            } else {
                                scope.launch { dismissState.reset() }
                            }
                        },
                    ) {
                        val state = layer.state
                        val handle = Modifier.draggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.GestureThresholdActivate
                                )
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.GestureEnd
                                )
                            }
                        )
                        InfoBar(
                            title = state.block.typeId,
                            description = "${state.block.typeId} ×${layer.height}".todoI18n(),
                            icon = {
                                AsyncImage(
                                    model = rememberBlockTextureRequest(state.block.typeId),
                                    contentDescription = null,
                                    modifier = handle.size(32.dp),
                                    filterQuality = FilterQuality.None
                                )
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    viewModel.selected = layer
                                    viewModel.picked = null
                                }
                                .padding(inset)
                                .applyInfoBarPadding()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DragHandle,
                                contentDescription = null,
                                modifier = handle
                            )
                        }
                    }
                }
            }
        }
        AnimatedBottomSheetDialog(
            targetState = viewModel.selected,
            enabledValues = HiddenOrExpanded
        ) { sheetState, selected ->
            var picking by rememberSaveable { mutableStateOf(false) }
            ModalBottomSheet(
                onDismissRequest = { viewModel.selected = null },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shape = OutlinedTextFieldDefaults.roundedShape
                    val textFieldState =
                        rememberTextFieldState(selected.height.toString())
                    BlockStatePreview(
                        state = viewModel.picked ?: selected.state,
                        shape = shape
                    ) {
                        picking = true
                    }
                    OutlinedTextField(
                        state = textFieldState,
                        shape = shape,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        label = { Text(stringResource(R.string.edit_layer_amount)) },
                        labelPosition = TextFieldLabelPosition.Cutout(isAlwaysMinimized = true),
                        placeholder = { Text(selected.height.toString()) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        inputTransformation = {
                            if (!this.asCharSequence().isDigitsOnly()) {
                                revertAllChanges()
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BottomSheetActionButton(
                            text = stringResource(android.R.string.cancel),
                            modifier = Modifier.weight(1.0F)
                        ) {
                            viewModel.selected = null
                        }
                        BottomSheetActionButton(
                            text = stringResource(android.R.string.ok),
                            modifier = Modifier.weight(1.0F)
                        ) {
                            viewModel.picked?.let { selected.state = it }
                            textFieldState.text.trim().toString().toIntOrNull()?.let {
                                selected.height = it
                            }
                            viewModel.selected = null
                        }
                    }
                }
            }
            if (picking) {
                PickBlockDialog(onCancel = { picking = false }) {
                    viewModel.picked = it
                    picking = false
                }
            }
        }
    }
}