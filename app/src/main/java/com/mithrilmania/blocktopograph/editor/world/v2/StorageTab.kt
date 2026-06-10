package com.mithrilmania.blocktopograph.editor.world.v2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.LocalPlayerSource
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.storage.file
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun StorageTab(
    viewModel: WorldEditorModel,
    info: InitState.Succeed
) {
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            TextButton("test") {
                coroutineScope.launch(Dispatchers.IO) {
                    val db = info.storage.db
                    val file = db.file(SpecialDBEntryType.LOCAL_PLAYER)
                    if (file.isPresent()) {
                        viewModel.editing.emit(
                            file to NBTImportConfigImpl(
                                NBTFormat.LITTLE_ENDIAN,
                                HeaderPresence.UNCERTAIN
                            )
                        )
                    } else {
                        viewModel.editing.emit(
                            LocalPlayerSource(
                                info.world.config
                            ) to NBTImportConfigImpl(
                                NBTFormat.LITTLE_ENDIAN,
                                HeaderPresence.PRESENT
                            )
                        )
                    }
                }
            }
        }
    }
}
