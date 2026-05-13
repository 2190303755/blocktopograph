package com.mithrilmania.blocktopograph.editor.nbt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuChip
import com.mithrilmania.blocktopograph.ui.component.InfoBar
import com.mithrilmania.blocktopograph.ui.component.applyInfoBarPadding

class NBTImportModel(
    val source: NBTSource,
    format: NBTFormat = NBTFormat.UNKNOWN,
    header: HeaderPresence = HeaderPresence.UNCERTAIN,
) : NBTImportConfig {
    override var format by mutableStateOf(format)
    override var header by mutableStateOf(header)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBTImportDialog(
    importer: NBTImportModel,
    state: SheetState,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InfoBar(
                title = "格式",
                modifier = Modifier.applyInfoBarPadding(horizontal = 32.dp)
            ) {
                DropdownMenuChip(
                    options = NBTFormat.entries,
                    selected = importer.format,
                    onSelect = { importer.format = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (it) {
                        NBTFormat.STRINGIFIED -> "SNBT"
                        NBTFormat.BIG_ENDIAN -> "大端序"
                        NBTFormat.LITTLE_ENDIAN -> "小端序"
                        else -> "未知"
                    }
                }
            }
            InfoBar(
                title = "标头",
                modifier = Modifier
                    .applyInfoBarPadding(horizontal = 32.dp)
            ) {
                DropdownMenuChip(
                    options = HeaderPresence.entries,
                    selected = if (importer.format.isHeaderAvailable) importer.header else HeaderPresence.ABSENT,
                    onSelect = { importer.header = it },
                    enabled = importer.format.isHeaderAvailable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (it) {
                        HeaderPresence.ABSENT -> "不存在"
                        HeaderPresence.PRESENT -> "存在"
                        else -> "自动检测"
                    }
                }
            }
            OutlinedButton(
                onClick = onImport,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text("导入")
            }
        }
    }
}