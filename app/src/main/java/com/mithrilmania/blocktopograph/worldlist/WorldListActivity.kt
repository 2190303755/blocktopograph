package com.mithrilmania.blocktopograph.worldlist

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.graphics.Insets
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
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorActivity
import com.mithrilmania.blocktopograph.util.FolderPicker
import com.mithrilmania.blocktopograph.util.WorldCreator
import com.mithrilmania.blocktopograph.util.asFolder
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.impl.loadSAFWorlds
import com.mithrilmania.blocktopograph.world.impl.loadShizukuWorlds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import kotlin.math.max

class WorldListActivity : BaseActivity() {
    private lateinit var selectMultipleWorlds: ActivityResultLauncher<Uri?>
    private lateinit var createWorld: ActivityResultLauncher<Unit>
    private lateinit var binding: ActivityWorldListBinding
    private lateinit var details: WorldDetailsDialog
    private val model by viewModels<WorldListModel>()
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val model = this.model
        this.binding = ActivityWorldListBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)
        this.setSupportActionBar(this.binding.toolbar)
        this.initLayoutManager()
        this.binding.worldList.setAdapter(this.model.adapter)
        this.selectMultipleWorlds = registerForActivityResult(FolderPicker) registry@{
            this.loadWorlds(it ?: return@registry)
        }
        this.createWorld = registerForActivityResult(WorldCreator) registry@{
            //this.adapter.addWorld(it ?: return@registry)
        }
        model.loading.observe(this) {
            if (it) this.binding.progress.show() else this.binding.progress.hide()
        }
        this.binding.fabLoadWorlds.setOnClickListener {
            this.selectMultipleWorlds.launch(null)
        }
        this.details = WorldDetailsDialog(this, model)
    }

    fun loadWorlds(location: Uri, tag: String = "") {
        val activity = this
        lifecycleScope.launch(Dispatchers.IO) {
            loadSAFWorlds(
                activity.model,
                activity.applicationContext,
                location.asFolder,
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

            R.id.action_open -> {
                val service = Blocktopograph.fileService
                if (service == null) {
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
                    return true
                }
                val activity = this
                val text = TextInputEditText(this)
                text.setText("/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang/minecraftWorlds/")
                MaterialAlertDialogBuilder(this)
                    .setTitle("Load Worlds")
                    .setView(text)
                    .setCancelable(false)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Next") { dialog, index ->
                        this.lifecycleScope.launch(Dispatchers.Default) {
                            loadShizukuWorlds(
                                activity.model,
                                activity.applicationContext,
                                text.text.toString(),
                            )
                        }
                    }
                    .show()
            }

            R.id.action_create -> this.createWorld.launch(Unit)
            R.id.action_edit -> this.startActivity(Intent(this, NBTEditorActivity::class.java))
            R.id.action_help -> this.upcoming()
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

    override fun applyContentInsets(window: View, insets: Insets) {
        val res = this.resources
        this.binding.fabLoadWorlds.applyFloatingInsets(insets) {
            res.getDimensionPixelSize(R.dimen.large_floating_margin)
        }
        this.binding.worldList.applyListInsets(
            this.isIndicatorEnabled,
            insets,
            res.getDimensionPixelSize(R.dimen.large_content_padding),
            res.getDimensionPixelSize(R.dimen.small_content_padding)
        )
    }
}