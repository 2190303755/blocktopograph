package com.mithrilmania.blocktopograph.worldlist

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityWorldListBinding
import com.mithrilmania.blocktopograph.util.FolderSelector
import com.mithrilmania.blocktopograph.util.asFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

class WorldListActivity : BaseActivity() {
    private lateinit var selectMultipleWorlds: ActivityResultLauncher<Uri?>
    private lateinit var adapter: WorldItemAdapter
    private lateinit var model: WorldListModel
    private lateinit var binding: ActivityWorldListBinding
    private lateinit var details: WorldDetailsDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.model = ViewModelProvider(this)[WorldListModel::class]
        this.binding = ActivityWorldListBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)
        this.setSupportActionBar(this.binding.toolbar)
        this.initLayoutManager()
        this.adapter = WorldItemAdapter(this.model)
        this.binding.worldList.setAdapter(this.adapter)
        this.selectMultipleWorlds = registerForActivityResult(FolderSelector()) {
            if (it != null) {
                this.loadWorlds(it)
            }
        }
        this.model.loading.observe(this) {
            this.binding.progressIndicator.apply {
                isIndeterminate = it
                visibility = if (it) View.VISIBLE else View.INVISIBLE
            }
        }
        this.binding.fabLoadWorlds.setOnClickListener {
            this.selectMultipleWorlds.launch(null)
        }
        this.details = WorldDetailsDialog(this, this.model)
    }

    fun loadWorlds(location: Uri, tag: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            loadWorlds(
                this@WorldListActivity.adapter,
                this@WorldListActivity,
                location.asFolder,
                this@WorldListActivity.model.loading,
                tag
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.world, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                val builder = MaterialAlertDialogBuilder(this)
                val msg = MaterialTextView(this)
                msg.ellipsize = TextUtils.TruncateAt.MARQUEE;
                val dpi = resources.displayMetrics.density
                val horizontal = (19 * dpi).toInt();
                val vertical = (5 * dpi).toInt();
                msg.setPadding(horizontal, vertical, horizontal, vertical);
                msg.setMaxLines(20)
                msg.movementMethod = LinkMovementMethod.getInstance()
                msg.setText(R.string.app_about)
                builder.setView(msg)
                    .setTitle(R.string.action_about)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                true
            }

            R.id.action_open -> {
                this.details.show()
                Toast.makeText(this, "测试", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_create -> {
                Toast.makeText(this, "前面的区域，以后再来探索吧！", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_help -> {
                Toast.makeText(this, "前面的区域，以后再来探索吧！", Toast.LENGTH_SHORT).show()
                true
            }

            else -> true
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        this.initLayoutManager()
    }

    fun initLayoutManager() {
        binding.worldList.layoutManager = GridLayoutManager(
            this, max(
                1,
                (0.003f * WindowMetricsCalculator.getOrCreate()
                    .computeCurrentWindowMetrics(this).bounds.width() / resources.displayMetrics.density).toInt()
            )
        )
    }

    override fun updateDecorViewPadding(decorView: View, systemBars: Insets, ime: Insets) {
        super.updateDecorViewPadding(decorView, systemBars, ime)
        val bottom = systemBars.bottom
        if (isIndicatorUsed) {
            binding.worldList.apply {
                updatePadding(
                    bottom = bottom + resources.getDimension(R.dimen.medium_content_padding).toInt()
                )
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = 0
                }
            }
        } else {
            binding.worldList.apply {
                updatePadding(
                    bottom = resources.getDimension(R.dimen.medium_content_padding).toInt()
                )
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottom
                }
            }
        }
        binding.fabLoadWorlds.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = bottom + resources.getDimension(R.dimen.fab_margin).toInt()
        }
    }
}