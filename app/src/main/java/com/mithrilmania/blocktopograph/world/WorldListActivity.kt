package com.mithrilmania.blocktopograph.world

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.zIndex
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ShizukuStatus
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorActivity
import com.mithrilmania.blocktopograph.editor.world.CreateWorldActivity
import com.mithrilmania.blocktopograph.ui.WorldDetailDialog
import com.mithrilmania.blocktopograph.ui.component.AnimatedBottomSheetDialog
import com.mithrilmania.blocktopograph.ui.component.HiddenOrExpanded
import com.mithrilmania.blocktopograph.ui.component.HorizontalPadding
import com.mithrilmania.blocktopograph.ui.component.PastableDialog
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.WorldItem
import com.mithrilmania.blocktopograph.ui.component.clickableItem
import com.mithrilmania.blocktopograph.ui.theme.setThemedContent
import com.mithrilmania.blocktopograph.util.asFolder
import com.mithrilmania.blocktopograph.util.collectText
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.upcoming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class WorldListActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        this.setThemedContent {
            val viewModel = viewModel<WorldListModel>()
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            var openViaShizuku by remember { mutableStateOf(false) }
            var showAbout by remember { mutableStateOf(false) }
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
                            val creator = rememberLauncherForActivityResult(
                                ActivityResultContracts.StartActivityForResult()
                            ) { result ->
                                val location = result.data.takeIf {
                                    result.resultCode == RESULT_OK
                                }?.data ?: return@rememberLauncherForActivityResult
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    viewModel.loadSAFWorld(location, this, "")
                                }
                            }
                            val context = LocalContext.current
                            val resources = LocalResources.current
                            AppBarRow(maxItemCount = 3) {
                                clickableItem(
                                    Icons.Filled.DriveFolderUpload,
                                    resources.getString(R.string.open)
                                ) {
                                    val service = Blocktopograph.fileService
                                    if (service === null) {
                                        when (Blocktopograph.getShizukuStatus()) {
                                            ShizukuStatus.UNAUTHORIZED -> {
                                                if (!Shizuku.shouldShowRequestPermissionRationale()) {
                                                    Shizuku.requestPermission(1)
                                                }
                                            }

                                            ShizukuStatus.UNSUPPORTED -> {
                                                Toast.makeText(
                                                    context,
                                                    "Shizuku版本过低",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            ShizukuStatus.UNKNOWN -> context.upcoming()

                                            ShizukuStatus.AVAILABLE -> {
                                                Toast.makeText(context, "!", Toast.LENGTH_SHORT)
                                                    .show()
                                            }
                                        }
                                    } else {
                                        openViaShizuku = true
                                    }
                                }
                                clickableItem(
                                    Icons.Filled.Edit,
                                    resources.getString(R.string.nbt_editor)
                                ) {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            NBTEditorActivity::class.java
                                        )
                                    )
                                }
                                clickableItem(
                                    Icons.Filled.Add,
                                    resources.getString(R.string.action_create)
                                ) {
                                    creator.launch(
                                        Intent(
                                            context,
                                            CreateWorldActivity::class.java
                                        )
                                    )
                                }
                                clickableItem(
                                    Icons.Filled.Info,
                                    resources.getString(R.string.action_about)
                                ) {
                                    showAbout = true
                                }
                                clickableItem(
                                    Icons.AutoMirrored.Filled.Help,
                                    resources.getString(R.string.action_help)
                                ) {
                                    context.upcoming()
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
                                viewModel.unscanned.send("" to folder.asFolder)
                            }
                        }
                        FloatingActionButton(
                            onClick = { picker.launch(null) },
                            modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
                        ) {
                            Icon(Icons.Filled.AddLocationAlt, tooltip)
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(PaddingValues(top = padding.calculateTopPadding()))) {
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
                    val edge = remember(padding) {
                        PaddingValues(
                            top = 6.dp,
                            start = 12.dp,
                            end = 12.dp,
                            bottom = 6.dp + padding.calculateBottomPadding()
                        ) + HorizontalPadding(padding)
                    }
                    val spacing = Arrangement.spacedBy(12.dp)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(300.dp),
                        contentPadding = edge,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = spacing,
                        horizontalArrangement = spacing
                    ) {
                        items(items = viewModel.worlds, key = { it.location.uid }) {
                            WorldItem(it, Modifier.animateItem()) {
                                viewModel.selected = it
                            }
                        }
                    }
                    AnimatedBottomSheetDialog(
                        targetState = viewModel.selected,
                        enabledValues = HiddenOrExpanded
                    ) { sheetState, selected ->
                        WorldDetailDialog(
                            detail = selected,
                            state = sheetState,
                            onDismiss = { viewModel.selected = null },
                        )
                    }
                }
            }

            if (showAbout) {
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
                    onDismissRequest = { showAbout = false },
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
                                .heightIn(max = with(LocalDensity.current) {
                                    LocalTextStyle.current.lineHeight.toDp() * 20
                                })
                                .verticalScroll(rememberScrollState())
                        )
                    },
                    confirmButton = {
                        TextButton(stringResource(android.R.string.ok)) {
                            showAbout = false
                        }
                    }
                )
            }

            if (openViaShizuku) {
                val input = rememberTextFieldState(defaultWorldPath())
                PastableDialog(
                    title = "加载世界",
                    onCancel = { openViaShizuku = false },
                    onPaste = {
                        val text = it?.collectText()
                        if (text === null) {
                            this@WorldListActivity.toast(R.string.toast_empty_clipboard)
                        } else {
                            input.setTextAndPlaceCursorAtEnd(text)
                        }
                    },
                    onConfirm = {
                        openViaShizuku = false
                        val service = Blocktopograph.fileService ?: return@PastableDialog
                        val path = input.text.toString()
                        if (path.isBlank() || !service.loadWorlds(path, viewModel.callback)) {
                            Toast.makeText(this, "invalid path", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    OutlinedTextField(input, shape = OutlinedTextFieldDefaults.roundedShape)
                }
            }
        }
    }
}