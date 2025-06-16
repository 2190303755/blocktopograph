package com.mithrilmania.blocktopograph.test

import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityNbtEditorBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTAdapter
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorView
import com.mithrilmania.blocktopograph.util.asCompleted
import com.mithrilmania.blocktopograph.util.upcoming
import net.benwoodworth.knbt.NbtByteArray
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtIntArray
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.buildNbtCompound
import net.benwoodworth.knbt.put
import net.benwoodworth.knbt.putNbtCompound

class NBTTestActivity : BaseActivity() {
    private lateinit var binding: ActivityNbtEditorBinding
    private lateinit var model: NBTEditorModel
    private lateinit var open: ActivityResultLauncher<Uri?>
    private lateinit var saveAs: ActivityResultLauncher<Uri?>
    private val requiringConfirmation = object : OnBackPressedCallback(false), Observer<Boolean> {
        override fun handleOnBackPressed() {
            MaterialAlertDialogBuilder(this@NBTTestActivity)
                .setTitle("更改未保存")
                .setMessage("如果不保存，您的更改将丢失。")
                .setNeutralButton("继续编辑", null)
                .setPositiveButton("保存") { dialog, which ->
                    this@NBTTestActivity.upcoming()
                }.setNegativeButton("不保存") { dialog, which ->
                    this@NBTTestActivity.finish()
                }.show()
        }

        override fun onChanged(value: Boolean) {
            this.isEnabled = value
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[NBTEditorModel::class]
        this.model = model
        val binding = ActivityNbtEditorBinding.inflate(this.layoutInflater)
        this.setContentView(binding.root)
        binding.toolbar.let {
            this.setSupportActionBar(it)
            it.setNavigationOnClickListener {
                this.onBackPressedDispatcher.onBackPressed()
            }
        }
        this.requiringConfirmation.let {
            this.onBackPressedDispatcher.addCallback(this, it)
            model.modified.observe(this, it)
        }
        binding.fabSave.setOnClickListener {
            this.binding.editor.adapter = NBTAdapter(this.model, "", buildNbtCompound {
                put("aaa", NbtInt(1))
                put("key", NbtString("value"))
                putNbtCompound("nested1") {
                    put("nested_aaa", NbtInt(1))
                    put("nested_key1", NbtString("value"))
                    put("ints", NbtIntArray(intArrayOf(1, 2, 3, 4, 5, 6)))
                    putNbtCompound("nested2") {
                        put("nested_bbb", NbtInt(1))
                        put("nested_key2", NbtString("value"))
                        put("ints", NbtIntArray(intArrayOf(1, 2, 3, 4, 5, 6)))
                    }
                }
                put("bytes", NbtByteArray(byteArrayOf(1, 2, 3, 4, 5, 6)))
            }.asCompleted())
        }


        this.binding = binding
        binding.editor.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = NBTAdapter(model, "", buildNbtCompound {
                put("test", 1)
                putNbtCompound("nested") {
                    put("nested_bbb", NbtInt(1))
                    put("nested_key", NbtString("value"))
                    put("ints", NbtIntArray(intArrayOf(1, 2, 3, 4, 5, 6)))
                }
            }.asCompleted())
            this.registerForContextMenu(it)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        info: ContextMenu.ContextMenuInfo?
    ) {
        if (info is NBTEditorView.ContextMenuInfo) {
            this.menuInflater.inflate(R.menu.nbt_node_options, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return super.onContextItemSelected(item)
    }
}