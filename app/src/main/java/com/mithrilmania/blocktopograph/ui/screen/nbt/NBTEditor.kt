package com.mithrilmania.blocktopograph.ui.screen.nbt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.ui.NBTImportModel
import com.mithrilmania.blocktopograph.ui.component.AnimatedExpanderIndicator
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.ui.component.safeLayoutInsets
import com.mithrilmania.blocktopograph.ui.rememberBridgedLazyListState
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.ui.screen.nbt.node.RootNode
import kotlinx.coroutines.launch

typealias ConfiguredNBTSource = Pair<NBTSource, NBTImportConfig>

fun NBTEditorModel.saveAsync() {
    val source = this.source
    if (source === null) {
        this.buildExporter()
    } else {
        this.viewModelScope.launch {
            saveToFile(source)
        }
    }
}

fun NBTEditorModel.confirmAction() {
    (this.confirmation ?: return).invoke()
    this.confirmation = null
}

fun NBTEditorModel.requestOrExecute(
    request: ConfirmationRequest
) {
    if (this.modified) {
        this.confirmation = request
    } else {
        request.invoke()
    }
}

fun NBTEditorModel.reload() {
    importer = NBTImportModel(
        source = source ?: return,
        header = if (littleEndian || storageVersion !== null) null else false,
        format = if (stringify) {
            NBTFormat.STRINGIFIED
        } else if (littleEndian) {
            NBTFormat.LITTLE_ENDIAN
        } else {
            NBTFormat.BIG_ENDIAN
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditor(
    sheetState: SheetState,
    textFieldState: TextFieldState,
    supportingText: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTransformation: InputTransformation? = null,
    onAction: KeyboardActionHandler
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            snapshotFlow { sheetState.currentValue }.collect {
                if (it == SheetValue.Expanded) {
                    focusRequester.requestFocus()
                }
            }
        }
        OutlinedTextField(
            state = textFieldState,
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusProperties {
                    canFocus = sheetState.currentValue === SheetValue.Expanded
                }
                .windowInsetsPadding(
                    safeLayoutInsets()
                        .only(WindowInsetsSides.Bottom)
                        .union(WindowInsets.ime)
                ),
            shape = OutlinedTextFieldDefaults.roundedShape,
            lineLimits = lineLimits,
            isError = isError,
            supportingText = supportingText,
            keyboardOptions = keyboardOptions,
            inputTransformation = inputTransformation,
            onKeyboardAction = onAction,
            trailingIcon = {
                IconButton(Icons.Filled.Check) {
                    onAction.onKeyboardAction {}
                }
            }
        )
    }
}

@Composable
fun NBTSummary(
    expandable: RootNode?,
    icon: Painter,
    key: String,
    summary: String,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = icon, contentDescription = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (expandable !== null) {
            AnimatedExpanderIndicator(
                expandable.expanded,
                SplitButtonDefaults.TrailingIconSize
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBTTree(
    padding: PaddingValues,
    editor: NBTEditorModel,
    sheetState: SheetState,
    modifier: Modifier
) {
    Box(modifier) {
        AnimatedVisibility(
            visible = editor.flattening,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(1.0F)
                .padding(padding.onlyTop())
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        val itemShape = MaterialTheme.shapes.small
        val coroutineScope = rememberCoroutineScope()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
            state = rememberBridgedLazyListState(coroutineScope),
            contentPadding = padding
        ) {
            items(
                items = editor.nodes,
                key = { it.uid },
                contentType = { it.type },
            ) { node ->
                val indent = Modifier
                    .padding(start = (node.depth * 32 - 16).dp)
                NBTSummary(
                    expandable = node as? RootNode,
                    icon = node.icon(),
                    key = node.key.toString(),
                    summary = node.summary(),
                    modifier = (if (node === editor.focused) indent.background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        itemShape
                    ) else indent)
                        .clip(itemShape)
                        .clickable {
                            val isFocused = editor.focused === node
                            if (node is RootNode && node.parent is NBTNode) {
                                val expanded = !node.expanded
                                if (expanded) {
                                    editor.expandNode(node)
                                    node.expanded = true
                                } else if (isFocused) {
                                    editor.collapsesNode(node)
                                    node.expanded = false
                                }
                            }
                            editor.focused = node
                            if (isFocused) {
                                coroutineScope.launch {
                                    sheetState.expand()
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .animateItem()
                )
            }
        }
    }
}
