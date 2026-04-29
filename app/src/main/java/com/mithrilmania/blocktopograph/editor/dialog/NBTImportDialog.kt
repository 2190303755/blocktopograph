package com.mithrilmania.blocktopograph.editor.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.google.common.primitives.Ints
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.JavaNBTInput
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.SNBTStreamReader
import com.mithrilmania.blocktopograph.nbt.io.TagWithMeta
import com.mithrilmania.blocktopograph.nbt.io.readNamedTag
import com.mithrilmania.blocktopograph.nbt.io.runSuppressing
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser
import com.mithrilmania.blocktopograph.nbt.util.parseSNBT
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuChip
import com.mithrilmania.blocktopograph.ui.component.InfoBar
import com.mithrilmania.blocktopograph.ui.component.applyInfoBarPadding
import com.mithrilmania.blocktopograph.util.autoDecompress
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

class NBTImportModel(
    val source: File,
    header: Boolean = true,
    format: NBTFormat = NBTFormat.UNKNOWN,
) {
    var format by mutableStateOf(format)
    var header by mutableStateOf(header)

    fun import(stream: InputStream): TagWithMeta? {
        val wrapped = stream.autoDecompress()
        when (this.format) {
            NBTFormat.STRINGIFIED -> {
                val pair = SNBTParser(SNBTStreamReader(wrapped)).parseRoot()
                return TagWithMeta(
                    pair.second,
                    pair.first,
                    wrapped is GZIPInputStream,
                    null,
                    littleEndian = false,
                    stringified = true
                )
            }

            NBTFormat.BIG_ENDIAN -> {
                val pair = JavaNBTInput(wrapped).readNamedTag()
                return TagWithMeta(
                    pair.second,
                    pair.first,
                    wrapped is GZIPInputStream,
                    null,
                    littleEndian = false
                )
            }

            NBTFormat.LITTLE_ENDIAN -> {
                if (this.header) {
                    val bytes = wrapped.readBytes()
                    if (bytes.size > 8 && bytes.size == 8 + Ints.fromBytes(
                            bytes[7],
                            bytes[6],
                            bytes[5],
                            bytes[4]
                        )
                    ) {
                        val pair = BedrockNBTInput(
                            ByteArrayInputStream(bytes, 8, bytes.size - 8)
                        ).readNamedTag()
                        return TagWithMeta(
                            pair.second,
                            pair.first,
                            wrapped is GZIPInputStream,
                            Ints.fromBytes(
                                bytes[3],
                                bytes[2],
                                bytes[1],
                                bytes[0]
                            ).toUInt(),
                            littleEndian = true
                        )
                    } else {
                        val pair = BedrockNBTInput(
                            ByteArrayInputStream(bytes)
                        ).readNamedTag()
                        return TagWithMeta(
                            pair.second,
                            pair.first,
                            wrapped is GZIPInputStream,
                            null,
                            littleEndian = true
                        )
                    }
                }
                val pair = BedrockNBTInput(wrapped).readNamedTag()
                return TagWithMeta(
                    pair.second,
                    pair.first,
                    wrapped is GZIPInputStream,
                    null,
                    littleEndian = true
                )
            }

            NBTFormat.UNKNOWN -> {
                val bytes = wrapped.readBytes()
                if (this.header) {
                    if (bytes.size > 8 && bytes.size == 8 + Ints.fromBytes(
                            bytes[7],
                            bytes[6],
                            bytes[5],
                            bytes[4]
                        )
                    ) runSuppressing {
                        val pair = BedrockNBTInput(
                            ByteArrayInputStream(bytes, 8, bytes.size - 8)
                        ).readNamedTag()
                        return TagWithMeta(
                            pair.second,
                            pair.first,
                            false,
                            Ints.fromBytes(
                                bytes[3],
                                bytes[2],
                                bytes[1],
                                bytes[0]
                            ).toUInt()
                        )
                    }
                }
                runSuppressing {
                    val pair = bytes.toString(Charsets.UTF_8).parseSNBT()
                    if (pair !== null) return TagWithMeta(
                        pair.second,
                        pair.first,
                        false,
                        null,
                        littleEndian = false,
                        stringified = true
                    )
                }
                runSuppressing {
                    val pair = BedrockNBTInput(
                        ByteArrayInputStream(bytes)
                    ).readNamedTag()
                    return TagWithMeta(
                        pair.second,
                        pair.first,
                        wrapped is GZIPInputStream,
                        null,
                        littleEndian = true
                    )
                }
                runSuppressing {
                    val pair = JavaNBTInput(
                        ByteArrayInputStream(bytes)
                    ).readNamedTag()
                    return TagWithMeta(
                        pair.second,
                        pair.first,
                        wrapped is GZIPInputStream,
                        null,
                        littleEndian = false
                    )
                }
            }
        }
        return null
    }
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
                title = "检测标头",
                modifier = Modifier
                    .toggleable(
                        value = importer.header,
                        enabled = importer.format.isHeaderAvailable,
                        onValueChange = { importer.header = it },
                        role = Role.Switch,
                    )
                    .applyInfoBarPadding(horizontal = 32.dp)
            ) {
                Switch(
                    checked = importer.format.isHeaderAvailable && importer.header,
                    enabled = importer.format.isHeaderAvailable,
                    onCheckedChange = null
                )
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