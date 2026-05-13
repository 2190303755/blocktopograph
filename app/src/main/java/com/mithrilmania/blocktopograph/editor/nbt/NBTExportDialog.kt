package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.MIME_SNBT
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuChip
import com.mithrilmania.blocktopograph.ui.component.InfoBar
import com.mithrilmania.blocktopograph.ui.component.applyInfoBarPadding
import com.mithrilmania.blocktopograph.util.FileCreator

class NBTExportModel(
    val source: NBTSource?,
    val repick: Boolean,
    stringify: Boolean,
    prettify: Boolean,
    heterogeneous: Boolean,
    compressed: Boolean,
    littleEndian: Boolean,
    version: UInt?
) : NBTExportConfig {
    var attachHeader by mutableStateOf(version != null)
    var version by mutableStateOf(version)
    override var stringify by mutableStateOf(stringify)
    override var prettify by mutableStateOf(prettify)
    override var heterogeneous by mutableStateOf(heterogeneous)
    override var compressed by mutableStateOf(compressed)
    override var littleEndian by mutableStateOf(littleEndian)
    override val storageVersion: UInt?
        get() = if (this.attachHeader) this.version else null

    fun buildOptions(context: Context) = FileCreator.Options(
        if (this.stringify) MIME_SNBT else MIME_TYPE_DEFAULT,
        (this.source as? SAFFile)?.uri,
        this.source?.resolveName(context) ?: ""
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBTExportDialog(
    exporter: NBTExportModel,
    creator: ActivityResultLauncher<FileCreator.Options?>,
    state: SheetState,
    onDismiss: () -> Unit,
    onExport: (NBTSource) -> Unit
) {
    val isHeaderAvailable = remember {
        derivedStateOf {
            exporter.version !== null
                    && exporter.littleEndian
                    && !exporter.stringify
                    && !exporter.compressed
        }
    }.value
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            val booleans = listOf(true, false)
            InfoBar(
                title = stringResource(R.string.dialog_storage_option_format),
                modifier = Modifier.applyInfoBarPadding(horizontal = 32.dp)
            ) {
                DropdownMenuChip(
                    options = booleans,
                    selected = exporter.stringify,
                    onSelect = { exporter.stringify = it },
                    modifier = Modifier.fillMaxWidth()
                ) { if (it) "SNBT" else "NBT" }
            }
            AnimatedContent(
                targetState = exporter.stringify,
                modifier = Modifier.padding(bottom = 12.dp)
            ) { stringify ->
                Column {
                    if (stringify) {
                        InfoBar(
                            title = stringResource(R.string.dialog_storage_option_prettify),
                            modifier = Modifier
                                .toggleable(
                                    value = exporter.prettify,
                                    onValueChange = { exporter.prettify = it },
                                    role = Role.Switch,
                                )
                                .applyInfoBarPadding(horizontal = 32.dp)
                        ) {
                            Switch(
                                checked = exporter.prettify,
                                onCheckedChange = null
                            )
                        }
                        InfoBar(
                            title = stringResource(R.string.dialog_storage_option_heterogeneous),
                            modifier = Modifier
                                .toggleable(
                                    value = exporter.heterogeneous,
                                    onValueChange = { exporter.heterogeneous = it },
                                    role = Role.Switch,
                                )
                                .applyInfoBarPadding(horizontal = 32.dp)
                        ) {
                            Switch(
                                checked = exporter.heterogeneous,
                                onCheckedChange = null
                            )
                        }
                    } else {
                        InfoBar(
                            title = stringResource(R.string.dialog_storage_option_endian),
                            modifier = Modifier.applyInfoBarPadding(horizontal = 32.dp)
                        ) {
                            DropdownMenuChip(
                                options = booleans,
                                selected = exporter.littleEndian,
                                onSelect = { exporter.littleEndian = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                stringResource(
                                    if (it) {
                                        R.string.dialog_storage_option_endian_little
                                    } else {
                                        R.string.dialog_storage_option_endian_big
                                    }
                                )
                            }
                        }
                        InfoBar(
                            title = stringResource(R.string.dialog_storage_option_compression),
                            modifier = Modifier
                                .toggleable(
                                    value = exporter.compressed,
                                    onValueChange = { exporter.compressed = it },
                                    role = Role.Switch,
                                )
                                .applyInfoBarPadding(horizontal = 32.dp)
                        ) {
                            Switch(
                                checked = exporter.compressed,
                                onCheckedChange = null
                            )
                        }
                        InfoBar(
                            title = stringResource(R.string.dialog_storage_option_header),
                            modifier = Modifier
                                .toggleable(
                                    value = exporter.attachHeader,
                                    enabled = isHeaderAvailable,
                                    onValueChange = { exporter.attachHeader = it },
                                    role = Role.Switch,
                                )
                                .applyInfoBarPadding(horizontal = 32.dp)
                        ) {
                            Switch(
                                checked = isHeaderAvailable && exporter.attachHeader,
                                enabled = isHeaderAvailable,
                                onCheckedChange = null
                            )
                        }
                    }
                }
            }
            val context = LocalContext.current
            OutlinedButton(
                onClick = {
                    val file = exporter.source
                    if (exporter.repick || file == null) {
                        creator.launch(exporter.buildOptions(context))
                    } else {
                        onExport(file)
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(stringResource(R.string.action_export))
            }
        }
    }
}