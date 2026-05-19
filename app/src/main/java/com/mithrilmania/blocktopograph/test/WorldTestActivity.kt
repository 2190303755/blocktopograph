package com.mithrilmania.blocktopograph.test

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditor
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.editor.nbt.NBTImportModel
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.runSuppressing
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.InfoBar
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.applyInfoBarPadding
import com.mithrilmania.blocktopograph.ui.component.applyInfoBoxPadding
import com.mithrilmania.blocktopograph.ui.component.showSnackbar
import com.mithrilmania.blocktopograph.ui.theme.setThemedContent
import com.mithrilmania.blocktopograph.util.ByteArrayMatcher
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.LEVEL_DB_TAG
import com.mithrilmania.blocktopograph.util.VIEW_DOCUMENT_FLAG
import com.mithrilmania.blocktopograph.util.errorAndPop
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.WorldModelFactory
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.await
import com.mithrilmania.blocktopograph.world.collectWorldCreationExtras
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorldTestActivity : ComponentActivity() {
    private val majorModel by viewModels<WorldTestModel>(
        this::collectWorldCreationExtras
    ) { WorldModelFactory(::WorldTestModel) }
    private val editor by viewModels<NBTEditorModel>()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        val majorModel = this.majorModel
        try {
            this.majorModel.open(this)
        } catch (e: Exception) {
            this.finish()
        }
        this.setThemedContent {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            val scaffoldState = rememberBottomSheetScaffoldState(
                rememberStandardBottomSheetState(
                    initialValue = SheetValue.Expanded
                )
            )
            val listState: LazyListState = rememberLazyListState()
            AnimatedContent(
                targetState = majorModel.editing
            ) { editing ->
                if (editing === null) {
                    val cutout = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    val scope = rememberCoroutineScope()
                    BottomSheetScaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            TopAppBar(
                                scrollBehavior = scrollBehavior,
                                title = {
                                    Text(stringResource(R.string.title_test_page))
                                },
                                navigationIcon = ::AppBarNavigationButton,
                                actions = {
                                    TooltipBox("repair") { tooltip ->
                                        IconButton(Icons.Filled.Build, tooltip) {
                                            upcoming()
                                        }
                                    }
                                }
                            )
                        },
                        snackbarHost = { SnackbarHost(majorModel.snackbar) },
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = with(LocalDensity.current) {
                            cutout.getBottom(this).toDp()
                        } + 36.dp,
                        sheetContent = {
                            AnimatedContent(
                                targetState = majorModel.isHexed,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                transitionSpec = { fadeIn() togetherWith fadeOut() }
                            ) { hexed ->
                                if (hexed) {
                                    OutlinedTextField(
                                        state = majorModel.hexedInput,
                                        lineLimits = TextFieldLineLimits.SingleLine,
                                        label = { Text("键") },
                                        modifier = Modifier.fillMaxWidth(),
                                        inputTransformation = InputTransformation.then {
                                            if (this.asCharSequence()
                                                    .startsWith("0x", ignoreCase = true)
                                            ) {
                                                delete(0, 2)
                                            }
                                        }.then {
                                            if (this.asCharSequence().any {
                                                    Character.digit(it.code, 16) < 0
                                                }
                                            ) {
                                                revertAllChanges()
                                            }
                                        },
                                        prefix = { Text("0x") },
                                        trailingIcon = {
                                            TooltipBox(stringResource(android.R.string.search_go)) { tooltip ->
                                                IconButton(Icons.Filled.Search, tooltip) {
                                                    val pattern = try {
                                                        majorModel.hexedInput.text.toString()
                                                            .hexToByteArray()
                                                    } catch (e: IllegalArgumentException) {
                                                        return@IconButton
                                                    }
                                                    majorModel.entries.clear()
                                                    scope.launch {
                                                        majorModel.storage.collectMatches(
                                                            pattern,
                                                            majorModel.entries
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    OutlinedTextField(
                                        state = majorModel.plainInput,
                                        lineLimits = TextFieldLineLimits.SingleLine,
                                        label = { Text("键") },
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            TooltipBox(stringResource(android.R.string.search_go)) { tooltip ->
                                                IconButton(Icons.Filled.Search, tooltip) {
                                                    val pattern = try {
                                                        majorModel.plainInput.text.toString()
                                                            .toByteArray(Charsets.UTF_8)
                                                    } catch (e: IllegalArgumentException) {
                                                        return@IconButton
                                                    }
                                                    majorModel.entries.clear()
                                                    scope.launch {
                                                        majorModel.storage.collectMatches(
                                                            pattern,
                                                            majorModel.entries
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            InfoBar(
                                title = "十六进制输入",
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .toggleable(
                                        value = majorModel.isHexed,
                                        onValueChange = { majorModel.isHexed = it },
                                        role = Role.Switch,
                                    )
                                    .applyInfoBarPadding()
                            ) {
                                Switch(
                                    checked = majorModel.isHexed,
                                    onCheckedChange = null
                                )
                            }
                            Spacer(Modifier.height(with(LocalDensity.current) {
                                cutout.getBottom(this).toDp()
                            }))
                        }
                    ) { padding ->
                        val creator = rememberLauncherForActivityResult(
                            FileCreator
                        ) callback@{ uri ->
                            if (uri === null) return@callback
                            val exporting = majorModel.exporting ?: return@callback
                            scope.launch(Dispatchers.IO) {
                                val bytes = exporting.db[exporting.key] ?: return@launch
                                try {
                                    this@WorldTestActivity.contentResolver.openOutputStream(uri)
                                        ?.use {
                                            it.write(bytes)
                                        }
                                } catch (e: Throwable) {
                                    errorAndPop(
                                        "Failed to query and export value with key ${exporting.key}",
                                        e,
                                        LEVEL_DB_TAG
                                    )
                                    return@launch
                                }
                                withContext(Dispatchers.Main) {
                                    val resources = this@WorldTestActivity.resources
                                    majorModel.snackbar.showSnackbar(
                                        message = resources.getString(R.string.world_test_export_done),
                                        actionLabel = resources.getString(R.string.world_test_open_file),
                                        duration = SnackbarDuration.Long
                                    ) {
                                        this@WorldTestActivity.startActivity(
                                            Intent()
                                                .setAction(Intent.ACTION_VIEW)
                                                .setFlags(VIEW_DOCUMENT_FLAG)
                                                .setDataAndType(uri, MIME_TYPE_DEFAULT)
                                        )
                                    }
                                }
                            }
                        }
                        LazyColumn(
                            state = listState,
                            contentPadding = padding,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(majorModel.entries) {
                                InfoBar(
                                    title = it.plainText,
                                    description = it.hexedText,
                                    modifier = Modifier.applyInfoBoxPadding()
                                ) {
                                    var expanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.wrapContentSize()) {
                                        SplitButtonLayout(
                                            leadingButton = {
                                                SplitButtonDefaults.TonalLeadingButton(onClick = {
                                                    majorModel.editing = it.toFile()
                                                }) {
                                                    Icon(
                                                        Icons.Filled.Edit,
                                                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                                        contentDescription = "Localized description",
                                                    )
                                                }
                                            },
                                            trailingButton = {
                                                TooltipBox("Toggle Button") { tooltip ->
                                                    SplitButtonDefaults.TonalTrailingButton(
                                                        checked = expanded,
                                                        onCheckedChange = { expanded = it },
                                                        modifier =
                                                            Modifier.semantics {
                                                                stateDescription =
                                                                    if (expanded) "Expanded" else "Collapsed"
                                                                contentDescription = tooltip
                                                            },
                                                    ) {
                                                        val rotation: Float by animateFloatAsState(
                                                            targetValue = if (expanded) 180f else 0f,
                                                            label = "Trailing Icon Rotation",
                                                        )
                                                        Icon(
                                                            Icons.Filled.KeyboardArrowDown,
                                                            modifier =
                                                                Modifier
                                                                    .size(SplitButtonDefaults.TrailingIconSize)
                                                                    .graphicsLayer {
                                                                        this.rotationZ = rotation
                                                                    },
                                                            contentDescription = "Localized description",
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.wrapContentSize()
                                        )
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Copy Key (Plain)") },
                                                onClick = { },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Filled.ContentCopy,
                                                        contentDescription = null
                                                    )
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Copy Key (Hexed)") },
                                                onClick = { },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Filled.ContentCopy,
                                                        contentDescription = null
                                                    )
                                                },
                                            )
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Export") },
                                                onClick = {
                                                    majorModel.exporting = it
                                                    creator.launch(null)
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Filled.Output,
                                                        contentDescription = null
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    BackHandler(true) {
                        majorModel.editing = null
                    }
                    NBTEditor(this@WorldTestActivity.editor) {
                        majorModel.editing = null
                    }
                }
            }
            LaunchedEffect(majorModel.editing) {
                majorModel.editing?.let {
                    this@WorldTestActivity.editor.readFromFile(
                        NBTImportModel(
                            it,
                            NBTFormat.LITTLE_ENDIAN,
                            HeaderPresence.UNCERTAIN
                        ),
                        this@WorldTestActivity
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Deferred<WorldStorage?>.collectMatches(
    pattern: ByteArray,
    destination: MutableList<LDBEntry>
) {
    flow {
        val db = try {
            this@collectMatches.await { it.db } ?: return@flow
        } catch (e: Throwable) {
            return@flow
        }
        val iterator = db.iterator()
        iterator.seekToFirst()
        if (pattern.isEmpty()) {
            while (iterator.hasNext()) {
                emit(LDBEntry(db, iterator.next().key))
            }
        } else {
            val failure = ByteArrayMatcher.computeFailure(pattern)
            while (iterator.hasNext()) {
                val key = iterator.next().key
                if (ByteArrayMatcher.contains(key, pattern, failure)) {
                    emit(LDBEntry(db, key))
                }
            }
        }
        runSuppressing {
            iterator.close()
        }
    }.flowOn(Dispatchers.IO)
        .chunked(5)
        .collect(destination::addAll)
}