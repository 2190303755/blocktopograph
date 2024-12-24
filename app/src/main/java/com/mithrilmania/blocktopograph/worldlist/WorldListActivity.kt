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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ShizukuStatus
import com.mithrilmania.blocktopograph.databinding.ActivityWorldListBinding
import com.mithrilmania.blocktopograph.util.FolderPicker
import com.mithrilmania.blocktopograph.util.WorldCreator
import com.mithrilmania.blocktopograph.util.asFolder
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.impl.SAFWorldLoader
import com.mithrilmania.blocktopograph.world.impl.ShizukuWorldLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import kotlin.math.max

class WorldListActivity : BaseActivity() {
    private lateinit var selectMultipleWorlds: ActivityResultLauncher<Uri?>
    private lateinit var createWorld: ActivityResultLauncher<Unit>
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
        this.selectMultipleWorlds = registerForActivityResult(FolderPicker) {
            if (it != null) {
                this.loadWorlds(it)
            }
        }
        this.createWorld = registerForActivityResult(WorldCreator) {
            if (it != null) {
                this.adapter.addWorld(it)
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
            SAFWorldLoader.loadWorlds(
                adapter,
                applicationContext,
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
        when (item.itemId) {
            R.id.action_about -> {
                val builder = MaterialAlertDialogBuilder(this)
                val msg = MaterialTextView(this)
                msg.ellipsize = TextUtils.TruncateAt.MARQUEE
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
            }

            R.id.action_open -> this.details.show()
            R.id.action_create -> this.createWorld.launch(Unit)
            R.id.action_help -> {
                val service = Blocktopograph.fileService
                if (service != null) {
                    val text = TextInputEditText(this)
                    text.setText("/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang/minecraftWorlds/")
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Load Worlds")
                        .setView(text)
                        .setCancelable(false)
                        .setNegativeButton("Cancel") { dialog, index ->

                        }
                        .setPositiveButton("Next") { dialog, index ->
                            this.lifecycleScope.launch(Dispatchers.Default) {
                                ShizukuWorldLoader.loadWorlds(
                                    adapter,
                                    applicationContext,
                                    text.text.toString(),
                                    model.loading
                                )
                            }
                        }
                        .show()
                    return true
                }
                when (Blocktopograph.getShizukuStatus()) {
                    ShizukuStatus.UNAUTHORIZED -> {
                        if (!Shizuku.shouldShowRequestPermissionRationale()) {
                            Shizuku.requestPermission(1)
                        }
                    }

                    ShizukuStatus.UNSUPPORTED -> {
                        Toast.makeText(this, "Shizuku版本过低", Toast.LENGTH_SHORT).show()
                    }

                    ShizukuStatus.UNKNOWN -> this.upcoming()

                    ShizukuStatus.AVAILABLE -> {
                        Toast.makeText(this, "!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        return true
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
        if (isIndicatorEnabled) {
            binding.worldList.apply {
                updatePadding(
                    bottom = bottom + resources.getDimension(R.dimen.large_content_padding).toInt()
                )
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = 0
                }
            }
        } else {
            binding.worldList.apply {
                updatePadding(
                    bottom = resources.getDimension(R.dimen.large_content_padding).toInt()
                )
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottom
                }
            }
        }
        binding.fabLoadWorlds.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = bottom + resources.getDimension(R.dimen.medium_floating_margin).toInt()
        }
    }
}