package com.mithrilmania.blocktopograph.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.ui.component.InfoBar
import com.mithrilmania.blocktopograph.ui.component.applyInfoBarPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

@Composable
fun BlockStatePreview(
    state: BlockTemplate,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    shape: Shape = MaterialTheme.shapes.small,
    onSelect: (BlockTemplate) -> Unit
) {
    InfoBar(
        title = state.block.name,
        description = state.subName ?: state.block.name,
        icon = {
            val icon = state.icon.getIcon(context)
            if (icon === null) {
                Spacer(Modifier.size(32.dp))
            } else {
                Icon(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
        },
        modifier = modifier
            .clickable { onSelect(state) }
            .background(
                Color(ColorUtils.blendARGB(state.color, 0x7f7f7f7f, 0.5f)),
                shape
            )
            .clip(shape)
            .applyInfoBarPadding()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickBlockDialog(
    onCancel: () -> Unit,
    onSelect: (BlockTemplate) -> Unit
) {
    BasicAlertDialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            BlockPicker(Modifier.padding(16.dp), onSelect)
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
fun BlockPicker(
    modifier: Modifier = Modifier,
    onSelect: (BlockTemplate) -> Unit
) {
    Column(modifier) {
        var templates: List<BlockTemplate> by remember { mutableStateOf(emptyList()) }
        val textFieldState = rememberTextFieldState()
        OutlinedTextField(
            state = textFieldState,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Filled.Search, null)
            }
        )
        LaunchedEffect(Unit) {
            snapshotFlow { textFieldState.text }.collectLatest { input ->
                templates = withContext(Dispatchers.Default) {
                    val keyword = input.trim()
                    if (keyword.isBlank()) {
                        BlockTemplates.getAll().collect(Collectors.toList())
                    } else {
                        BlockTemplates.getAll().filter {
                            it.block.name.contains(keyword, ignoreCase = true)
                                    || (it.subName?.contains(keyword, ignoreCase = true) ?: false)
                        }.collect(Collectors.toList())
                    }
                }
            }
        }
        val context = LocalContext.current
        LazyColumn(Modifier.sizeIn(maxHeight = 512.dp)) {
            items(
                items = templates,
                key = { it.block.name + it.block.hashCode() }
            ) {
                BlockStatePreview(it, Modifier.animateItem(), context, RectangleShape, onSelect)
            }
        }
    }
}