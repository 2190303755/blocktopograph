package com.mithrilmania.blocktopograph.editor.world

import android.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mithrilmania.blocktopograph.LogUtil
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.WorldActivity
import com.mithrilmania.blocktopograph.editor.dialog.NBTImportModel
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorFragment
import com.mithrilmania.blocktopograph.map.Dimension
import com.mithrilmania.blocktopograph.map.TileEntity
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.nbt.io.HeaderPresence
import com.mithrilmania.blocktopograph.nbt.io.LocalPlayerSource
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.storage.VirtualFile
import com.mithrilmania.blocktopograph.storage.file
import com.mithrilmania.blocktopograph.util.LEVEL_DB_TAG
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.popAndTransit
import com.mithrilmania.blocktopograph.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorldEditorActivity : WorldActivity() {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        LogUtil.d(this, "World activity nav-drawer menu item selected: $id")
        val drawer = mBinding.drawerLayout

        when (id) {
            R.id.nav_world_show_map -> changeContentFragment(this::openWorldMap)
            R.id.nav_world_select -> closeWorldActivity()
            R.id.nav_singleplayer_nbt -> openLocalPlayer()
            R.id.nav_multiplayer_nbt -> openMultiplayerEditor()
            R.id.nav_world_nbt -> openLevelEditor()
            R.id.nav_overworld_satellite -> this.model.navigateTo(
                Dimension.OVERWORLD,
                MapType.OVERWORLD_SATELLITE
            )

            R.id.nav_overworld_cave -> this.model.navigateTo(
                Dimension.OVERWORLD,
                MapType.OVERWORLD_CAVE
            )

            R.id.nav_overworld_slime_chunk -> this.model.navigateTo(
                Dimension.OVERWORLD,
                MapType.OVERWORLD_SLIME_CHUNK
            )

            R.id.nav_overworld_heightmap -> this.model.navigateTo(
                Dimension.OVERWORLD,
                MapType.OVERWORLD_HEIGHTMAP
            )

            R.id.nav_overworld_biome -> this.model.navigateTo(
                Dimension.OVERWORLD,
                MapType.OVERWORLD_BIOME
            )

            R.id.nav_overworld_grass -> this.model.navigateTo(
                Dimension.OVERWORLD,
                MapType.OVERWORLD_GRASS
            )

            R.id.nav_overworld_xray -> this.model.navigateTo(
                Dimension.OVERWORLD,
                MapType.OVERWORLD_XRAY
            )

            R.id.nav_overworld_block_light -> this.model.navigateTo(
                Dimension.OVERWORLD,
                MapType.OVERWORLD_BLOCK_LIGHT
            )

            R.id.nav_nether_map -> this.model.navigateTo(Dimension.NETHER, MapType.NETHER)
            R.id.nav_nether_xray -> this.model.navigateTo(Dimension.NETHER, MapType.NETHER_XRAY)
            R.id.nav_nether_block_light -> this.model.navigateTo(
                Dimension.NETHER,
                MapType.NETHER_BLOCK_LIGHT
            )

            R.id.nav_nether_biome -> this.model.navigateTo(Dimension.NETHER, MapType.NETHER_BIOME)
            R.id.nav_end_satellite -> this.model.navigateTo(Dimension.END, MapType.END_SATELLITE)
            R.id.nav_end_heightmap -> this.model.navigateTo(Dimension.END, MapType.END_HEIGHTMAP)
            R.id.nav_end_block_light -> this.model.navigateTo(
                Dimension.END,
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

            R.id.nav_biomedata_nbt -> changeContentFragment {
                openSpecialDBEntry(SpecialDBEntryType.BIOME_DATA)
            }

            R.id.nav_overworld_nbt -> changeContentFragment {
                openSpecialDBEntry(SpecialDBEntryType.OVERWORLD)
            }

            R.id.nav_villages_nbt -> changeContentFragment {
                openSpecialDBEntry(SpecialDBEntryType.M_VILLAGES)
            }

            R.id.nav_portals_nbt -> changeContentFragment {
                openSpecialDBEntry(SpecialDBEntryType.PORTALS)
            }

            R.id.nav_dimension0_nbt -> changeContentFragment {
                openSpecialDBEntry(SpecialDBEntryType.DIMENSION_0)
            }

            R.id.nav_dimension1_nbt -> changeContentFragment {
                openSpecialDBEntry(SpecialDBEntryType.DIMENSION_1)
            }

            R.id.nav_dimension2_nbt -> changeContentFragment {
                openSpecialDBEntry(SpecialDBEntryType.DIMENSION_2)
            }

            R.id.nav_autonomous_entities_nbt -> changeContentFragment {
                openSpecialDBEntry(SpecialDBEntryType.AUTONOMOUS_ENTITIES)
            }

            R.id.nav_open_nbt_by_name -> this.openCustomEntry()
            else ->  //Warning, we might have messed with the menu XML!
                LogUtil.d(this, "pressed unknown navigation-item in world-activity-drawer")
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Loads local player data "~local-player" or level.dat>"Player" into an EditableNBT.
     */
    override fun openLocalPlayer() {
        val handler = this.model?.handler ?: return
        val db = handler.storage?.db
        if (db !== null) {
            this.lifecycleScope.launch(Dispatchers.IO) {
                if (openIfPresent(db.file(SpecialDBEntryType.LOCAL_PLAYER))) return@launch
                withContext(Dispatchers.Main) {
                    checkAndOpenNBTEditor(
                        LocalPlayerSource(handler.config),
                        HeaderPresence.PRESENT
                    )
                }
            }
        } else {
            this.checkAndOpenNBTEditor(
                LocalPlayerSource(handler.config),
                HeaderPresence.PRESENT
            )
        }
    }

    override fun openLevelEditor() {
        this.checkAndOpenNBTEditor(
            this.model?.handler?.config ?: return,
            HeaderPresence.PRESENT
        )
    }

    override fun openCustomEntry() {
        val keyInput = EditText(this).apply {
            setEms(16)
            setMaxEms(32)
            setHint(R.string.leveldb_key_here)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.open_nbt_from_db)
            .setView(keyInput)
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.open) click@{ dialog, _ ->
                val activity = this
                val key = keyInput.getText().toString()
                if (key.isEmpty()) {
                    Snackbar.make(
                        this.mBinding?.drawerLayout ?: return@click,
                        R.string.invalid_keyname,
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null).show();
                } else {
                    val db = this.model?.handler?.storage?.db ?: return@click
                    this.lifecycleScope.launch(Dispatchers.IO) {
                        if (activity.openIfPresent(db.file(key))) return@launch
                        activity.notifyMissingKey(key)
                    }
                }
            }.show()
    }

    override fun openSpecialDBEntry(entry: SpecialDBEntryType?) {
        if (entry === null) return
        val db = this.model?.handler?.storage?.db ?: return
        val activity = this
        this.lifecycleScope.launch(Dispatchers.IO) {
            if (activity.openIfPresent(db.file(entry))) return@launch
            activity.notifyMissingKey(entry.keyName)
        }
    }

    override fun openMultiplayerEditor() {
        val storage = this.model?.handler?.storage ?: return
        val activity = this
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(ProgressBar(activity).apply {
                isIndeterminate = true
            })
            .show()
        this.lifecycleScope.launch(Dispatchers.IO) {
            val players: List<String> = try {
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
                    ).setAction("Action", null).show()
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                val spinner = Spinner(activity)
                spinner.adapter =
                    ArrayAdapter(activity, android.R.layout.simple_spinner_item, players)
                AlertDialog.Builder(activity)
                    .setTitle(R.string.select_player)
                    .setView(spinner)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.open_nbt) click@{ dialog, _ ->
                        val player = players.getOrNull(spinner.selectedItemPosition) ?: return@click
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            if (activity.openIfPresent(storage.db.file(player))) return@launch
                            activity.notifyMissingKey(player)
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
        header: HeaderPresence = HeaderPresence.UNCERTAIN
    ) {
        val importer = NBTImportModel(source, NBTFormat.LITTLE_ENDIAN, header)
        // confirmContentClose shouldn't be both used as boolean and as close-message,
        //  this is a bad pattern
        if (this.confirmContentClose === null) {
            this.openNBTEditor(importer)
            return
        }
        AlertDialog.Builder(this)
            .setMessage(this.confirmContentClose)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                this.openNBTEditor(importer)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun openNBTEditor(importer: NBTImportModel) {
        this.supportFragmentManager.popAndTransit {
            replace(R.id.world_content, NBTEditorFragment(importer))
            addToBackStack(null)
        }
    }

    fun notifyDBFailure(key: String) {
        Snackbar.make(
            this.mBinding?.root ?: return,
            this.getString(R.string.failed_read_player_from_db_with_key_x, key),
            Snackbar.LENGTH_LONG
        ).setAction("Action", null).show()
    }

    fun notifyMissingKey(key: String) {
        Snackbar.make(
            this.mBinding?.root ?: return,
            "Missing key: '$key'", // TODO i18n
            Snackbar.LENGTH_LONG
        ).show()
    }
}