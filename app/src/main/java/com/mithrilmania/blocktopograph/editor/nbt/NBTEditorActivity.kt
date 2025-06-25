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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.EXTRA_INVALIDATED
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityNbtEditorBinding
import com.mithrilmania.blocktopograph.editor.nbt.holder.NodeHolder
import com.mithrilmania.blocktopograph.editor.nbt.holder.RootHolder
import com.mithrilmania.blocktopograph.editor.nbt.node.ListNode
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.editor.nbt.node.stringify
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.util.FilePicker
import com.mithrilmania.blocktopograph.util.applyFloatingInsets
import com.mithrilmania.blocktopograph.util.applyListInsets
import com.mithrilmania.blocktopograph.util.clipboard
import com.mithrilmania.blocktopograph.util.showIfAbsent
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.upcoming

class NBTEditorActivity : BaseActivity() {
    private lateinit var binding: ActivityNbtEditorBinding
    private lateinit var open: ActivityResultLauncher<Uri?>
    private val model by viewModels<NBTEditorModel>()
    private val requiringConfirmation = object : OnBackPressedCallback(false), Observer<Boolean> {
        override fun handleOnBackPressed() {
            // TODO i18n
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
        binding.appBar.let {
            this.setSupportActionBar(it)
            it.setNavigationOnClickListener {
                this.onBackPressedDispatcher.onBackPressed()
            }
        }
        binding.editor.let {
            it.layoutManager = LinearLayoutManager(this)
            this.registerForContextMenu(it)
        }
        model.tree.observe(this) {
            this.invalidateOptionsMenu()
            this.binding.apply {
                if (it === null) {
                    save.isEnabled = false
                    search.isEnabled = false
                    return@observe
                }
                save.isEnabled = true
                search.isEnabled = true
                editor.adapter = it
            }
            it.reloadAsync()
        }
        model.version.observe(this) {
            this.binding.appBar.subtitle = if (it == null) null else
                this.getString(R.string.activity_nbt_editor_subtitle, it.toLong())
        }
        model.loading.observe(this) {
            if (it) this.binding.progress.show() else this.binding.progress.hide()
        }
        model.source.observe(this) {
            this.title = it?.getName(this)
                ?: this.resources.getString(R.string.nbt_editor)
        }
        model.history.observe(this) {
            this.binding.apply {
                undo.isEnabled = it.undo
                redo.isEnabled = it.redo
            }
        }
        this.requiringConfirmation.let {
            this.onBackPressedDispatcher.addCallback(this, it)
            model.modified.observe(this, it)
        }
        binding.undo.setOnClickListener callback@{
            this.model.undo()
        }
        binding.redo.setOnClickListener callback@{
            this.model.redo()
        }
        binding.search.setOnClickListener {
            this.upcoming()
        }
        binding.save.setOnClickListener {
            this.saveFile()
        }
        this.open = registerForActivityResult(FilePicker) registry@{
            this.model.readFileAsync(SAFFile(it ?: return@registry), this)
        }
        if (model.source.isInitialized) return
        this.onNewIntent(this.intent)
    }

    fun saveFile() {
        val model = this.model
        val source = model.source.value
        if (source === null) {
            this.showIfAbsent(NBTExportDialog.TAG, ::NBTExportDialog)
        } else {
            model.saveFileAsync(source, this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                this.model.readFileAsync(
                    if (uri == null) {
                        ShizukuFile(intent.getStringExtra(EXTRA_PATH) ?: return)
                    } else {
                        SAFFile(uri)
                    },
                    this
                )
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View?, info: ContextMenuInfo?) {
        if (info !is NodeHolder<*, *>) return
        menu.add(R.string.action_copy).setOnMenuItemClickListener callback@{
            val holder = it.menuInfo as? NodeHolder<*, *> ?: return@callback true
            val node = holder.node ?: return@callback true
            holder.context.clipboard?.apply {
                setPrimaryClip(ClipData.newPlainText("Copy", node.stringify()))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    holder.context.toast(R.string.toast_copy_success)
                }
            }
            true
        }
        val holding = info.node
        val parent = holding?.parent
        when (parent) {
            is MapNode -> {
                menu.add(R.string.action_rename).setOnMenuItemClickListener callback@{
                    val node = (it.menuInfo as? NodeHolder<*, *>)?.node ?: return@callback true
                    val parent = node.parent as? MapNode ?: return@callback true
                    this.renameNode(node.name, parent) rename@{ old, neo ->
                        if (old == neo) return@rename
                        parent.remove(old)
                        node.name = neo
                        parent.put(neo, node)
                        this.model += Rename(node, parent, old, neo)
                    }
                    true
                }
                menu.add(R.string.action_replace).setOnMenuItemClickListener callback@{
                    val old = (it.menuInfo as? NodeHolder<*, *>)?.node ?: return@callback true
                    val parent = old.parent as? MapNode ?: return@callback true
                    this.replaceNode(parent, old) { neo ->
                        parent.remove(old.name)
                        parent.put(neo.name, neo)
                        this.model += Replace(old, parent, neo)
                        if (parent.expanded) {
                            parent.tree.reloadAsync()
                        }
                    }
                    true
                }
            }

            is ListNode -> {
                val index = parent.indexOf(holding)
                if (index > 0) {
                    menu.add(R.string.action_move_up).setOnMenuItemClickListener callback@{
                        val node = (it.menuInfo as? NodeHolder<*, *>)?.node ?: return@callback true
                        val parent = node.parent as? ListNode ?: return@callback true
                        val index = parent.indexOf(node)
                        if (index > 0 && parent.swap(index, index - 1)) {
                            this.model += Move(node, parent, index, index - 1)
                            parent.notifyMovedChildren()
                        }
                        true
                    }
                }
                if (index + 1 < parent.size) {
                    menu.add(R.string.action_move_dowm).setOnMenuItemClickListener callback@{
                        val node = (it.menuInfo as? NodeHolder<*, *>)?.node ?: return@callback true
                        val parent = node.parent as? ListNode ?: return@callback true
                        val index = parent.indexOf(node)
                        if (index >= 0 && parent.swap(index, index + 1)) {
                            this.model += Move(node, parent, index, index + 1)
                            parent.notifyMovedChildren()
                        }
                        true
                    }
                }
            }

            else -> {}
        }
        when (holding) {
            is NBTTree -> {
                menu.add(R.string.action_rename).setOnMenuItemClickListener callback@{
                    val node = (it.menuInfo as? RootHolder)?.node ?: return@callback true
                    this.renameNode(this.model.name, null) rename@{ old, neo ->
                        if (old == neo) return@rename
                        this.model.name = neo
                        this.model += Relabel(node, old, neo)
                    }
                    true
                }
                holding.makeInsertOption(menu.add(R.string.action_insert), this)
                return
            }

            is RootNode<*> -> holding.makeInsertOption(menu.add(R.string.action_insert), this)
        }
        menu.add(R.string.action_delete).setOnMenuItemClickListener callback@{
            val node = (it.menuInfo as? NodeHolder<*, *>)?.node ?: return@callback true
            val parent = node.parent
            when (parent) {
                is ListNode -> {
                    val index = parent.indexOf(node)
                    if (index < 0 || parent.remove(index) == null) return@callback true
                    this.model += Delete<Int>(node, parent, index)
                }

                is MapNode -> {
                    if (parent.remove(node.name) == null) return@callback true
                    this.model += Delete<String>(node, parent, node.name)
                }
                else -> return@callback true
            }
            if (parent.expanded) {
                this.model.tree.value?.reloadAsync()
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menuInflater.inflate(R.menu.activity_nbt_editor, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_file_create -> this.model.reset()
            R.id.action_file_open -> this.open.launch(null)
            R.id.action_file_save -> this.saveFile()
            R.id.action_file_save_as -> this.showIfAbsent(NBTExportDialog.TAG) {
                NBTExportDialog().apply {
                    arguments = Bundle().apply {
                        putBoolean(EXTRA_INVALIDATED, true)
                    }
                }
            }
            R.id.action_file_reload, R.id.action_info -> this.upcoming()
            R.id.action_quit -> this.onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        this.model.apply {
            val hasFile = source.value !== null
            val notEmpty = tree.value !== null
            menu.apply {
                findItem(R.id.action_file_save).isEnabled = notEmpty
                findItem(R.id.action_file_save_as).isEnabled = notEmpty
                findItem(R.id.action_file_reload).isEnabled = hasFile
                findItem(R.id.action_info).isEnabled = hasFile
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun applyContentInsets(window: View, insets: Insets) {
        val res = this.resources
        val margin = res.getDimensionPixelSize(R.dimen.small_floating_margin)
        binding.toolbar.applyFloatingInsets(insets, margin)
        this.binding.editor.applyListInsets(
            this.isIndicatorEnabled,
            insets,
            res.getDimensionPixelSize(R.dimen.editor_extra_padding) + margin
        )
    }
}