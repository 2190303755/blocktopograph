package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_DEFAULT_FORMAT
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_DETECT_HEADER
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_SKIP_IMPORTER
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.editor.dialog.NBTImportModel
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.ui.theme.setThemedContent
import com.mithrilmania.blocktopograph.util.toEnum
import kotlinx.coroutines.launch

class NBTEditorActivity : ComponentActivity() {
    private val viewModel by viewModels<NBTEditorModel>()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        if (savedInstanceState === null) {
            this.onNewIntent(this.intent)
        }
        this.setThemedContent {
            NBTEditor(this.viewModel, this::finish)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                val format = intent.getStringExtra(EXTRA_EDITOR_DEFAULT_FORMAT)
                    ?.uppercase()
                    .toEnum(NBTFormat.UNKNOWN)
                val importer = NBTImportModel(
                    if (uri == null) {
                        ShizukuFile(intent.getStringExtra(EXTRA_PATH) ?: return)
                    } else {
                        SAFFile(uri)
                    },
                    header = if (format.isHeaderAvailable
                        && intent.getBooleanExtra(EXTRA_EDITOR_DETECT_HEADER, true)
                    ) {
                        HeaderPresence.UNCERTAIN
                    } else {
                        HeaderPresence.ABSENT
                    },
                    format = format
                )
                if (intent.getBooleanExtra(EXTRA_EDITOR_SKIP_IMPORTER, false)) {
                    this.viewModel.apply {
                        viewModelScope.launch {
                            readFromFile(importer, this@NBTEditorActivity)
                        }
                    }
                } else {
                    this.viewModel.importer = importer
                }
            }
        }
    }
}