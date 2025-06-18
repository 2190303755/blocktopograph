package com.mithrilmania.blocktopograph.editor.nbt

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityNbtEditorBinding
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.RootHolder
import com.mithrilmania.blocktopograph.editor.nbt.node.ListNode
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.stringify
import com.mithrilmania.blocktopograph.nbt.readUnknownNBT
import com.mithrilmania.blocktopograph.nbt.writeCompound
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.FilePicker
import com.mithrilmania.blocktopograph.util.clipboard
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.BUNDLE_ENTRY_PATH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream


class NBTEditorActivity : BaseActivity() {
    private lateinit var binding: ActivityNbtEditorBinding
    private lateinit var open: ActivityResultLauncher<Uri?>
    private lateinit var saveAs: ActivityResultLauncher<Uri?>
    private val model by viewModels<NBTEditorModel>()
    private var adapter: NBTAdapter? = null
    private val requiringConfirmation = object : OnBackPressedCallback(false), Observer<Boolean> {
        override fun handleOnBackPressed() {
            MaterialAlertDialogBuilder(this@NBTEditorActivity)
                .setTitle("更改未保存")
                .setMessage("如果不保存，您的更改将丢失。")
                .setNeutralButton("继续编辑", null)
                .setPositiveButton("保存") { dialog, which ->
                    this@NBTEditorActivity.saveFile()
                }.setNegativeButton("不保存") { dialog, which ->
                    this@NBTEditorActivity.finish()
                }.show()
        }

        override fun onChanged(value: Boolean) {
            this.isEnabled = value
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val model = this.model
        val binding = ActivityNbtEditorBinding.inflate(this.layoutInflater)
        this.binding = binding
        this.setContentView(binding.root)
        this.setSupportActionBar(binding.toolbar)
        binding.editor.let {
            it.layoutManager = LinearLayoutManager(this)
            this.registerForContextMenu(it)
        }
        model.version.observe(this) {
            this.binding.toolbar.subtitle = if (it == null) null else
                this.getString(R.string.activity_nbt_editor_subtitle, it)
        }
        model.data.observe(this) {
            this.adapter = NBTAdapter(this.model, it)
            this.binding.editor.adapter = this.adapter
        }
        model.loading.observe(this) {
            if (it) this.binding.progress.show() else this.binding.progress.hide()
        }
        model.source.observe(this) {
            this.title = it?.getName(this)
                ?: this.resources.getString(R.string.nbt_editor)
        }
        this.requiringConfirmation.let {
            this.onBackPressedDispatcher.addCallback(this, it)
            model.modified.observe(this, it)
        }
        binding.fabSave.setOnClickListener {
            this.saveFile()
        }
        this.saveAs = registerForActivityResult(FileCreator) registry@{
            this.model.source.value = SAFFile(it ?: return@registry)
            this.saveFile()
        }
        this.open = registerForActivityResult(FilePicker) registry@{
            this.model.source.value = SAFFile(it ?: return@registry)
            this.readFileAsync()
        }
        if (model.source.value == null) {
            this.onNewIntent(this.intent)
        }
    }

    fun readFileAsync() {
        this.model.loading.value = true
        this.invalidateOptionsMenu()
        val activity = this
        this.lifecycleScope.launch(Dispatchers.IO) {
            activity.model.apply {
                accept(source.value?.read(activity, InputStream::readUnknownNBT) ?: return@launch)
            }
        }
    }

    fun saveFileAsync() {
        val activity = this // to avoid label
        this.lifecycleScope.launch(Dispatchers.Default) {
            val data = activity.adapter?.asTag() ?: return@launch
            withContext(Dispatchers.IO) {
                activity.model.apply {
                    source.value?.save(activity) { stream ->
                        version.value?.let {
                            stream.writeCompound(it, data, name)
                        } ?: stream.writeCompound(data, name)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "Done", Toast.LENGTH_SHORT).show()
                activity.model.modified.value = false
            }
        }
    }

    fun saveFile() {
        if (this.model.source.value != null) return this.saveFileAsync()
        this.upcoming()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                this.model.source.value = if (uri == null) {
                    ShizukuFile(intent.getStringExtra(BUNDLE_ENTRY_PATH) ?: return)
                } else {
                    SAFFile(uri)
                }
                this.readFileAsync()
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View?, info: ContextMenuInfo?) {
        if (info !is NodeHolder<*, *>) return
        menu.add(R.string.action_copy).setOnMenuItemClickListener callback@{
            val node = (it.menuInfo as? NodeHolder<*, *>)?.node ?: return@callback true
            this@NBTEditorActivity.clipboard?.apply {
                setPrimaryClip(ClipData.newPlainText("Copy", node.stringify()))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    this@NBTEditorActivity.toast(R.string.toast_copy_success)
                }
            }
            true
        }
        val holding = info.node
        if (holding?.parent is MapNode) {
            menu.add(R.string.action_rename).setOnMenuItemClickListener callback@{
                val holder = it.menuInfo as? NodeHolder<*, *> ?: return@callback true
                val node = holder.node ?: return@callback true
                val parent = node.parent as? MapNode ?: return@callback true
                this.renameNode(node.name) {
                    parent.remove(node.name)
                    holder.rename(it)
                    parent.put(it, node)
                    this.model.modified.value = true
                }
                true
            }
        }
        when (holding) {
            is NBTAdapter -> {
                menu.add(R.string.action_rename).setOnMenuItemClickListener callback@{
                    val holder = it.menuInfo as? RootHolder ?: return@callback true
                    this.renameNode(this.model.name, holder::rename)
                    this.model.modified.value = true
                    true
                }
                menu.add(R.string.action_insert).setOnMenuItemClickListener callback@{
                    this.upcoming()
                    true
                }
                return
            }

            is ListNode -> menu.add(R.string.action_insert).setOnMenuItemClickListener callback@{
                this.upcoming()
                true
            }

            is MapNode -> {
                menu.add(R.string.action_insert).setOnMenuItemClickListener callback@{
                    this.upcoming()
                    true
                }
            }
        }
        menu.add(R.string.action_replace).setOnMenuItemClickListener callback@{
            this.upcoming()
            true
        }
        menu.add(R.string.action_delete).setOnMenuItemClickListener callback@{
            val node = (it.menuInfo as? NodeHolder<*, *>)?.node ?: return@callback true
            val parent = node.parent
            when (parent) {
                is ListNode -> if (!parent.remove(node)) return@callback true
                is MapNode -> if (parent.remove(node.name) == null) return@callback true
                else -> return@callback true
            }
            this.model.modified.value = true
            this.adapter?.reloadAsync()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menuInflater.inflate(R.menu.activity_nbt_editor, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_file_create -> {
                model.reset()
                invalidateOptionsMenu()
            }

            R.id.action_file_open -> this.open.launch(null)
            R.id.action_file_save -> this.saveFile()
            R.id.action_file_save_as -> this.saveAs.launch(null)
            R.id.action_test -> this.upcoming()
            R.id.action_quit -> this.onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val hasFile = model.source.value != null
        menu.apply {
            findItem(R.id.action_file_save).isEnabled = hasFile
            findItem(R.id.action_file_save_as).isEnabled = hasFile
            findItem(R.id.action_file_reload).isEnabled = hasFile
            findItem(R.id.action_info).isEnabled = hasFile
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun updateDecorViewPadding(decorView: View, systemBars: Insets, ime: Insets) {
        super.updateDecorViewPadding(decorView, systemBars, ime)
        val bottom = systemBars.bottom
        binding.editor.apply {
            if (isIndicatorEnabled) {
                updatePadding(bottom = bottom + resources.getDimensionPixelSize(R.dimen.large_content_padding))
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = 0
                }
            } else {
                updatePadding(bottom = resources.getDimensionPixelSize(R.dimen.large_content_padding))
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottom
                }
            }
        }
        this.binding.fabSave.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = bottom + resources.getDimensionPixelSize(R.dimen.medium_floating_margin)
        }
    }
}