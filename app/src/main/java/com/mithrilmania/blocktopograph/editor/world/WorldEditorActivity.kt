package com.mithrilmania.blocktopograph.editor.world

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.android.material.snackbar.Snackbar
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.MIME_TYPE_ZIP
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.WorldActivity
import com.mithrilmania.blocktopograph.map.MapFragment
import com.mithrilmania.blocktopograph.map.TileEntity
import com.mithrilmania.blocktopograph.map.edit.EditResultCode
import com.mithrilmania.blocktopograph.map.edit.SearchAndReplaceDialog
import com.mithrilmania.blocktopograph.map.picer.PicerDialog
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.nbt.io.LocalPlayerSource
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.storage.VirtualFile
import com.mithrilmania.blocktopograph.storage.wrapAsFile
import com.mithrilmania.blocktopograph.ui.AppNavKey.NBTEditor
import com.mithrilmania.blocktopograph.ui.BlankNBTEditor
import com.mithrilmania.blocktopograph.ui.LocalResourcePackViewModel
import com.mithrilmania.blocktopograph.ui.ResourcePackViewModel
import com.mithrilmania.blocktopograph.ui.component.AlertDialog
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.SwipeableSnackbar
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.navigateBack
import com.mithrilmania.blocktopograph.ui.screen.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.ui.screen.nbt.NBTEditorScreen
import com.mithrilmania.blocktopograph.ui.screen.world.LevelDBKey
import com.mithrilmania.blocktopograph.ui.screen.world.wrapAsFile
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme
import com.mithrilmania.blocktopograph.util.LEVEL_DB_TAG
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.VIEW_DOCUMENT_FLAG
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.util.tryOrNull
import com.mithrilmania.blocktopograph.world.StandaloneKey
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.wrapAsFile
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.AlertDialog.Builder as AlertDialogBuilder

class WorldEditorActivity : WorldActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        this.lifecycleScope.launch {
            model.showDrawer.collect {
                mBinding.drawerLayout.openDrawer(mBinding.navView, true)
            }
        }
        this.lifecycleScope.launch {
            for (markers in model.pendingMarkers) {
                markers.forEach { marker ->
                    marker.view?.let { view ->
                        (view.parent as? ViewGroup)?.removeView(view)
                    }
                    mapFragment.addMarker(marker)
                }
            }
        }
        this.lifecycleScope.launch {
            for (intent in model.pendingIntent) {
                startActivity(intent)
            }
        }
        this.lifecycleScope.launch {
            for (camera in model.pendingMovement) {
                mapFragment.frameTo(camera.x, camera.y)
            }
        }
        this.lifecycleScope.launch {
            model.editResult.collect {
                if (it === EditResultCode.SUCCESS) {
                    toast(R.string.general_done)
                    mapFragment.refreshAfterEdit()
                } else {
                    toast(R.string.general_failed)
                }
            }
        }
        this.findViewById<ComposeView>(R.id.composer).setContent {
            BlocktopographCompatTheme {
                CompositionLocalProvider(
                    LocalResourcePackViewModel provides viewModel<ResourcePackViewModel>()
                ) {
                    SelectionObserver(model)
                    val backStack = model.backStack
                    NavDisplay(
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        backStack = backStack,
                        transitionSpec = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start
                            ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                        },
                        popTransitionSpec = {
                            EnterTransition.None togetherWith slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End
                            )
                        },
                        predictivePopTransitionSpec = {
                            EnterTransition.None togetherWith slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End
                            )
                        }
                    ) { key ->
                        if (key is NBTEditor && key !== BlankNBTEditor) NavEntry(
                            key = key,
                            metadata = metadata {
                                if (key.crossfade) {
                                    put(NavDisplay.TransitionKey) {
                                        fadeIn() togetherWith fadeOut()
                                    }
                                }
                            }
                        ) {
                            NBTEditorScreen(
                                spec = key,
                                onNew = {
                                    backStack[
                                        backStack.lastIndex
                                    ] = NBTEditor(null, crossfade = true)
                                },
                                onExit = {
                                    backStack.navigateBack()
                                }
                            ) {
                                NBTEditorModel(it, key, model.modified, model.confirmation).apply {
                                    modified = false
                                    confirmation = null
                                }
                            }
                        } else NavEntry(key) {
                            Box(Modifier.fillMaxSize()) {
                                val viewer = model
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomStart),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    SnackbarHost(viewer.snackbar) {
                                        SwipeableSnackbar(it)
                                    }
                                    WorldEditorMenu(
                                        viewer,
                                        worldModel,
                                        Modifier,
                                        mapFragment
                                    ) {
                                        val preferences = getPreferences(MODE_PRIVATE)
                                        if (!preferences.getBoolean(
                                                MapFragment.KEY_HAS_DOUBLE_TAP,
                                                false
                                            )
                                        ) {
                                            preferences.edit {
                                                putBoolean(
                                                    MapFragment.KEY_HAS_DOUBLE_TAP,
                                                    true
                                                )
                                            }
                                            toast(R.string.map_dblclick_notice)
                                        }
                                    }
                                }
                                FloatingPanes(viewer, worldModel)
                                LongPressDialog(viewer, mapFragment)
                                MapFragmentDialogs(viewer, mapFragment)
                                Text(
                                    stringResource(R.string.map_water_mark),
                                    Modifier
                                        .padding(4.dp)
                                        .align(Alignment.BottomStart),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Light,
                                    color = colorResource(R.color.waterMark),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        shadow = Shadow(
                                            color = colorResource(R.color.waterMarkShadow),
                                            offset = Offset(2.0F, 2.0F),
                                            blurRadius = 2.0F
                                        )
                                    )
                                )
                                PicerDialog(viewer, worldModel)
                                SearchAndReplaceDialog(viewer)
                                if (viewer.waitingJob) {
                                    BasicAlertDialog(onDismissRequest = { viewer.cancelBlockingJob() }) {
                                        Column(
                                            Modifier
                                                .clip(AlertDialogDefaults.shape)
                                                .background(AlertDialogDefaults.containerColor)
                                                .padding(24.dp)
                                        ) {
                                            Text(
                                                stringResource(R.string.general_please_wait),
                                                color = AlertDialogDefaults.titleContentColor,
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                            CircularProgressIndicator(
                                                Modifier
                                                    .padding(top = 16.dp)
                                                    .align(Alignment.CenterHorizontally)
                                            )
                                        }
                                    }
                                }
                                if (viewer.confirmCloseWorld) {
                                    val onDismissRequest = {
                                        viewer.confirmCloseWorld = false
                                    }
                                    AlertDialog(
                                        onDismissRequest = onDismissRequest,
                                        title = {
                                            Text(stringResource(R.string.confirm_close_world))
                                        },
                                        neutralButton = {
                                            TextButton(
                                                stringResource(android.R.string.cancel),
                                                onClick = onDismissRequest
                                            )
                                        },
                                        negativeButton = {
                                            TextButton("暂离".todoI18n()) {
                                                viewer.confirmCloseWorld = false
                                                viewer.waitForJob(lifecycleScope.launch {
                                                    mapFragment.closeChunks()
                                                    finish()
                                                })
                                            }
                                        },
                                        positiveButton = {
                                            TextButton(stringResource(android.R.string.ok)) {
                                                viewer.confirmCloseWorld = false
                                                viewer.waitForJob(lifecycleScope.launch {
                                                    mapFragment.closeChunks()
                                                    withContext(Dispatchers.IO) {
                                                        worldModel.awaitStorage()?.path?.let {
                                                            worldModel.world.merge(it)
                                                        }
                                                    }?.let {
                                                        startActivity(
                                                            Intent()
                                                                .setAction(Intent.ACTION_VIEW)
                                                                .setFlags(VIEW_DOCUMENT_FLAG)
                                                                .setDataAndType(it, MIME_TYPE_ZIP)
                                                        )
                                                    }
                                                    finish()
                                                })
                                            }
                                        }
                                    ) {}
                                }
                                if (model.showOpenByNameDialog) {
                                    OpenNBTByNameDialog(
                                        storage = worldModel.storage,
                                        onDismiss = {
                                            model.showOpenByNameDialog = false
                                        },
                                        onSelect = { file ->
                                            model.showOpenByNameDialog = false
                                            checkAndOpenNBTEditor(file)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SelectionObserver(viewer: WorldViewerModel) {
        val selection = viewer.selection
        SideEffect(
            selection.left,
            selection.top,
            selection.right,
            selection.bottom
        ) {
            mapFragment?.selectionBoard?.requestLayout()
        }
        SideEffect(viewer.paneType) {
            mapFragment?.selectionBoard?.isVisible = viewer.paneType == PaneType.SELECTOR
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        LogUtil.d(this, "World activity nav-drawer menu item selected: $id")

        when (id) {
            R.id.nav_world_show_map -> model.requestOrExecute {
                model.backStack.navigateBack()
            }
            R.id.nav_world_select -> model.confirmCloseWorld = true
            R.id.nav_singleplayer_nbt -> openLocalPlayer()
            R.id.nav_multiplayer_nbt -> openMultiplayerEditor()
            R.id.nav_world_nbt -> openLevelEditor()
            R.id.nav_overworld_satellite -> this.model.navigateTo(
                VanillaDimension.OVERWORLD,
                MapType.OVERWORLD_SATELLITE
            )

            R.id.nav_overworld_cave -> this.model.navigateTo(
                VanillaDimension.OVERWORLD,
                MapType.OVERWORLD_CAVE
            )

            R.id.nav_overworld_slime_chunk -> this.model.navigateTo(
                VanillaDimension.OVERWORLD,
                MapType.OVERWORLD_SLIME_CHUNK
            )

            R.id.nav_overworld_heightmap -> this.model.navigateTo(
                VanillaDimension.OVERWORLD,
                MapType.OVERWORLD_HEIGHTMAP
            )

            R.id.nav_overworld_biome -> this.model.navigateTo(
                VanillaDimension.OVERWORLD,
                MapType.OVERWORLD_BIOME
            )

            R.id.nav_overworld_grass -> this.model.navigateTo(
                VanillaDimension.OVERWORLD,
                MapType.OVERWORLD_GRASS
            )

            R.id.nav_overworld_xray -> this.model.navigateTo(
                VanillaDimension.OVERWORLD,
                MapType.OVERWORLD_XRAY
            )

            R.id.nav_overworld_block_light -> this.model.navigateTo(
                VanillaDimension.OVERWORLD,
                MapType.OVERWORLD_BLOCK_LIGHT
            )

            R.id.nav_nether_map -> this.model.navigateTo(VanillaDimension.NETHER, MapType.NETHER)
            R.id.nav_nether_xray -> this.model.navigateTo(
                VanillaDimension.NETHER,
                MapType.NETHER_XRAY
            )
            R.id.nav_nether_block_light -> this.model.navigateTo(
                VanillaDimension.NETHER,
                MapType.NETHER_BLOCK_LIGHT
            )

            R.id.nav_nether_biome -> this.model.navigateTo(
                VanillaDimension.NETHER,
                MapType.NETHER_BIOME
            )

            R.id.nav_end_satellite -> this.model.navigateTo(
                VanillaDimension.END,
                MapType.END_SATELLITE
            )

            R.id.nav_end_heightmap -> this.model.navigateTo(
                VanillaDimension.END,
                MapType.END_HEIGHTMAP
            )
            R.id.nav_end_block_light -> this.model.navigateTo(
                VanillaDimension.END,
                MapType.END_BLOCK_LIGHT
            )

            R.id.nav_map_opt_toggle_grid ->  //toggle the grid
                this.model.showGrid.value = false == this.model.showGrid.value

            R.id.nav_map_opt_filter_markers -> {
                //toggle the grid
                TileEntity.loadIcons(this.assets)
                this.mapFragment.openMarkerFilter()
            }

            R.id.nav_map_opt_toggle_markers -> {
                //toggle markers
                val visible = false == this.model.showMarkers.getValue()
                this.model.showMarkers.value = visible
                this.getPreferences(MODE_PRIVATE).edit {
                    putBoolean(PREF_KEY_SHOW_MARKERS, visible)
                }
            }

            R.id.nav_biomedata_nbt -> openStandaloneEntry(StandaloneKey.BIOME_DATA)

            R.id.nav_overworld_nbt -> openSpecialDBEntry(SpecialDBEntryType.OVERWORLD)

            R.id.nav_villages_nbt -> openStandaloneEntry(StandaloneKey.LEGACY_VILLAGE)

            R.id.nav_portals_nbt -> openStandaloneEntry(StandaloneKey.PORTALS)

            R.id.nav_dimension0_nbt -> openSpecialDBEntry(SpecialDBEntryType.DIMENSION_0)

            R.id.nav_dimension1_nbt -> openSpecialDBEntry(SpecialDBEntryType.DIMENSION_1)

            R.id.nav_dimension2_nbt -> openSpecialDBEntry(SpecialDBEntryType.DIMENSION_2)

            R.id.nav_autonomous_entities_nbt -> openStandaloneEntry(StandaloneKey.AUTONOMOUS_ENTITIES)

            R.id.nav_open_nbt_by_name -> this.openCustomEntry()
            else ->  //Warning, we might have messed with the menu XML!
                LogUtil.d(this, "pressed unknown navigation-item in world-activity-drawer")
        }

        mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Loads local player data "~local-player" or level.dat>"Player" into an EditableNBT.
     */
    fun openLocalPlayer() {
        val model = this.worldModel ?: return
        this.lifecycleScope.launch(Dispatchers.IO) {
            val db = model.awaitStorage()?.db ?: return@launch
            if (openIfPresent(db.wrapAsFile(StandaloneKey.LOCAL_PLAYER))) return@launch
            withContext(Dispatchers.Main) {
                checkAndOpenNBTEditor(
                    LocalPlayerSource(model.world.config),
                    true
                )
            }
        }
    }

    fun openLevelEditor() {
        this.checkAndOpenNBTEditor(
            this.worldModel?.world?.config ?: return,
            true
        )
    }

    fun openCustomEntry() {
        model.showOpenByNameDialog = true
    }

    /**
     * Open NBT editor fragment for special database entry
     */
    fun openSpecialDBEntry(entry: SpecialDBEntryType?) {
        if (entry === null) return
        val activity = this
        this.lifecycleScope.launch(Dispatchers.IO) {
            val db = activity.worldModel?.awaitStorage()?.db ?: return@launch
            if (activity.openIfPresent(db.wrapAsFile(entry))) return@launch
            activity.notifyMissingKey(entry.keyName)
        }
    }

    /**
     * Open NBT editor screen for special database entry
     */
    fun openStandaloneEntry(key: StandaloneKey) {
        val activity = this
        this.lifecycleScope.launch(Dispatchers.IO) {
            val db = activity.worldModel?.awaitStorage()?.db ?: return@launch
            if (activity.openIfPresent(db.wrapAsFile(key))) return@launch
            activity.notifyMissingKey(key.literal)
        }
    }


    fun openMultiplayerEditor() {
        val dialog = AlertDialogBuilder(this)
            .setCancelable(false)
            .setView(ProgressBar(this).apply {
                isIndeterminate = true
            })
            .show()
        val activity = this
        this.lifecycleScope.launch(Dispatchers.IO) {
            val storage = try {
                activity.worldModel?.storage?.await() ?: return@launch
            } catch (e: Exception) {
                Log.e(LEVEL_DB_TAG, "Failed to open world", e)
                withContext(Dispatchers.Main) {
                    activity.toast(R.string.error_general)
                    dialog.dismiss()
                }
                return@launch
            }
            val players: List<ByteArray> = try {
                storage.networkPlayerNameList
            } catch (e: Exception) {
                Log.e(LEVEL_DB_TAG, "Failed to load player list", e)
                withContext(Dispatchers.Main) {
                    activity.toast(R.string.error_general)
                    dialog.dismiss()
                }
                return@launch
            }
            if (players.isEmpty()) {
                withContext(Dispatchers.Main) feedback@{
                    dialog.dismiss()
                    Snackbar.make(
                        activity.mBinding?.root ?: return@feedback,
                        R.string.no_multiplayer_data_found,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                val spinner = Spinner(activity)
                spinner.adapter = ArrayAdapter(
                    activity,
                    android.R.layout.simple_spinner_item,
                    players.map { it.toString(Charsets.UTF_8) }
                )
                AlertDialogBuilder(activity)
                    .setTitle(R.string.select_player)
                    .setView(spinner)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.open_nbt) click@{ _, _ ->
                        val player = players.getOrNull(spinner.selectedItemPosition) ?: return@click
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            val name = player.toString(Charsets.UTF_8)
                            if (!activity.openIfPresent(
                                    storage.db.wrapAsFile(
                                        name,
                                        key = player
                                    )
                                )
                            ) {
                                activity.notifyMissingKey(name)
                            }
                        }
                    }.show()
            }
        }
    }

    suspend fun openIfPresent(file: VirtualFile): Boolean {
        if (file.isPresent()) {
            withContext(Dispatchers.Main) {
                this@WorldEditorActivity.checkAndOpenNBTEditor(file)
            }
            return true
        }
        return false
    }

    fun checkAndOpenNBTEditor(
        source: NBTSource,
        header: Boolean? = null
    ) {
        model.checkAndOpenNBTEditor(
            source,
            NBTImportConfigImpl(NBTFormat.LITTLE_ENDIAN, header)
        )
    }

    fun notifyDBFailure(key: String) {
        Snackbar.make(
            this.mBinding?.root ?: return,
            this.getString(R.string.failed_read_player_from_db_with_key_x, key),
            Snackbar.LENGTH_LONG
        ).show()
    }

    fun notifyMissingKey(key: String) {
        Snackbar.make(
            this.mBinding?.root ?: return,
            "Missing key: '$key'".todoI18n(),
            Snackbar.LENGTH_LONG
        ).show()
    }
}

@Composable
private fun OpenNBTByNameDialog(
    storage: Deferred<WorldStorage?>,
    onDismiss: () -> Unit,
    onSelect: (VirtualFile) -> Unit
) {
    val input = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    val matches = remember { mutableStateListOf<LevelDBKey>() }

    fun search() {
        if (searching) return
        val pattern = input.text.toString().toByteArray(Charsets.UTF_8)
        if (pattern.isEmpty()) return
        searching = true
        searched = false
        matches.clear()
        scope.launch {
            storage.collectMatches(pattern, matches)
            searching = false
            searched = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.open_nbt_from_db)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    state = input,
                    modifier = Modifier.fillMaxWidth(),
                    shape = OutlinedTextFieldDefaults.roundedShape,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text(stringResource(R.string.leveldb_key_here)) },
                    trailingIcon = {
                        IconButton(Icons.Filled.Search) { search() }
                    }
                )
                when {
                    searching -> CircularProgressIndicator(
                        Modifier
                            .padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    searched && matches.isEmpty() -> Text(
                        text = stringResource(R.string.cannot_find_db_entry_with_name),
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    matches.isNotEmpty() -> LazyColumn(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 320.dp)
                    ) {
                        items(matches, key = { it.hexed }) { entry ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            onSelect((tryOrNull {
                                                storage.await()
                                            } ?: return@launch).db.wrapAsFile(entry))
                                        }
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(entry.plain, maxLines = 1)
                                Text(
                                    text = entry.plain,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                text = stringResource(android.R.string.search_go),
                enabled = !searching && input.text.isNotEmpty()
            ) {
                search()
            }
        },
        dismissButton = {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss
            )
        }
    )
}