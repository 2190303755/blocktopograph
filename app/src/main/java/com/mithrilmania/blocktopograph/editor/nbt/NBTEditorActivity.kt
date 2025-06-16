package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityNbtEditorBinding
import com.mithrilmania.blocktopograph.nbt.readUnknownNBT
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.storage.ShizukuFile
import com.mithrilmania.blocktopograph.util.asCompleted
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.BUNDLE_ENTRY_PATH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class NBTEditorActivity : BaseActivity() {
    private lateinit var binding: ActivityNbtEditorBinding
    private lateinit var model: NBTEditorModel

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val model = ViewModelProvider(this)[NBTEditorModel::class]
        this.model = model
        val binding = ActivityNbtEditorBinding.inflate(this.layoutInflater)
        this.binding = binding
        this.setContentView(binding.root)
        binding.toolbar.let {
            this.setSupportActionBar(it)
            it.setNavigationOnClickListener {
                this.finish()
            }
        }
        binding.fabSave.setOnClickListener {
            this.upcoming()
        }
        binding.editor.apply {
            layoutManager = LinearLayoutManager(this@NBTEditorActivity)
        }
        this.onNewIntent(this.intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                val source = if (uri == null) {
                    ShizukuFile(intent.getStringExtra(BUNDLE_ENTRY_PATH) ?: return)
                } else {
                    SAFFile(this, uri)
                }
                this.invalidateOptionsMenu()
                this.model.source = source
                this.lifecycleScope.launch(Dispatchers.IO) {
                    source.read(InputStream::readUnknownNBT)?.let { parsed ->
                        withContext(Dispatchers.Main) {
                            binding.apply {
                                toolbar.subtitle = parsed.version?.let {
                                    this@NBTEditorActivity.getString(
                                        R.string.activity_nbt_editor_subtitle,
                                        it
                                    )
                                }
                                editor.adapter = NBTAdapter(
                                    this@NBTEditorActivity.model,
                                    parsed.name,
                                    parsed.data.asCompleted()
                                )
                            }
                        }
                    }
                }
            }
        }
        this.intent = null
    }

    override fun updateDecorViewPadding(decorView: View, systemBars: Insets, ime: Insets) {
        super.updateDecorViewPadding(decorView, systemBars, ime)
        this.binding.fabSave.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin =
                systemBars.bottom + resources.getDimensionPixelSize(R.dimen.medium_floating_margin)
        }
    }
}