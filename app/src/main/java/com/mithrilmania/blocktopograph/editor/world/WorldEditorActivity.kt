package com.mithrilmania.blocktopograph.editor.world

import android.app.AlertDialog
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.WorldActivity
import com.mithrilmania.blocktopograph.nbt.EditableNBT
import com.mithrilmania.blocktopograph.nbt.EditorFragment
import com.mithrilmania.blocktopograph.nbt.LevelDat
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType
import com.mithrilmania.blocktopograph.util.getAsEditableNBT
import com.mithrilmania.blocktopograph.util.popAndTransit
import com.mithrilmania.blocktopograph.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorldEditorActivity : WorldActivity() {
    /**
     * Opens an editableNBT for just the subTag if it is not null.
     * Opens the whole level.dat if subTag is null.
     **/
    fun prepareLevelDat(subTag: String? = null): EditableNBT? {
        val root = this.model?.handler?.getDataCompat(this)?.deepCopy ?: return null
        val tag = if (subTag == null) null
        else (root.getChildTagByKey(subTag) ?: return null)
        return object : LevelDat(root, tag) {
            override fun save(): Boolean {
                val activity = this@WorldEditorActivity
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    activity.model?.handler?.save(activity, root)
                }
                return true
            }
        }
    }

    /**
     * Loads local player data "~local-player" or level.dat>"Player" into an EditableNBT.
     */
    override fun openLocalPlayer() {
        val db = this.model?.handler?.storage?.db
        val activity = this
        this.lifecycleScope.launch(Dispatchers.IO) {
            val nbt = db?.getAsEditableNBT(SpecialDBEntryType.LOCAL_PLAYER)
                ?: activity.prepareLevelDat("Player")
            withContext(Dispatchers.Main) {
                if (nbt === null) {
                    this@WorldEditorActivity.toast(R.string.missing_local_player)
                    return@withContext
                }
                this@WorldEditorActivity.checkAndOpenNBTEditor(nbt)
            }
        }
    }

    override fun openLevelEditor() {
        this.lifecycleScope.launch(Dispatchers.IO) {
            val nbt = this@WorldEditorActivity.prepareLevelDat()
            if (nbt === null) {
                this@WorldEditorActivity.toast(R.string.error_general)
                return@launch
            }
            withContext(Dispatchers.Main) {
                this@WorldEditorActivity.checkAndOpenNBTEditor(nbt)
            }
        }
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
                        val nbt = db.getAsEditableNBT(key) {
                            activity.notifyDBFailure(it)
                        } ?: return@launch
                        withContext(Dispatchers.Main) {
                            activity.checkAndOpenNBTEditor(nbt)
                        }
                    }
                }
            }.show()
    }

    override fun openSpecialDBEntry(entry: SpecialDBEntryType?) {
        if (entry === null) return
        val db = this.model?.handler?.storage?.db ?: return
        val activity = this
        this.lifecycleScope.launch(Dispatchers.IO) {
            val nbt = db.getAsEditableNBT(entry) {
                activity.notifyDBFailure(it)
            } ?: return@launch
            withContext(Dispatchers.Main) {
                activity.checkAndOpenNBTEditor(nbt)
            }
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
                Log.e("LevelDB", "Failed to load player list", e)
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
                            val nbt = storage.db.getAsEditableNBT(player) {
                                activity.notifyDBFailure(it)
                            } ?: return@launch
                            withContext(Dispatchers.Main) {
                                this@WorldEditorActivity.checkAndOpenNBTEditor(nbt)
                            }
                        }
                    }.show()
            }
        }
    }

    fun checkAndOpenNBTEditor(nbt: EditableNBT) {
        // confirmContentClose shouldn't be both used as boolean and as close-message,
        //  this is a bad pattern
        if (this.confirmContentClose === null) {
            this.openNBTEditor(nbt)
            return
        }
        AlertDialog.Builder(this)
            .setMessage(this.confirmContentClose)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                this.openNBTEditor(nbt)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun openNBTEditor(nbt: EditableNBT) {
        // see changeContentFragment(callback)
        this.confirmContentClose = this.getString(R.string.confirm_close_nbt_editor)
        this.supportFragmentManager.popAndTransit {
            replace(R.id.world_content, EditorFragment(nbt))
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
}