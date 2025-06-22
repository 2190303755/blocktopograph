package com.mithrilmania.blocktopograph.editor.world

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityCreateWorldBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTOptions
import com.mithrilmania.blocktopograph.flat.EditFlatFragment
import com.mithrilmania.blocktopograph.flat.FlatLayers
import com.mithrilmania.blocktopograph.flat.Layer
import com.mithrilmania.blocktopograph.nbt.Keys.FLAT_WORLD_LAYERS
import com.mithrilmania.blocktopograph.nbt.asTag
import com.mithrilmania.blocktopograph.nbt.convert.LevelDataConverter.skip
import com.mithrilmania.blocktopograph.nbt.modifyAsCompound
import com.mithrilmania.blocktopograph.nbt.readCompound
import com.mithrilmania.blocktopograph.nbt.unwrap
import com.mithrilmania.blocktopograph.util.BiomePicker
import com.mithrilmania.blocktopograph.util.FolderPicker
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_TIME
import com.mithrilmania.blocktopograph.world.KEY_LEVEL_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtLong

class CreateWorldActivity : BaseActivity() {
    private lateinit var binding: ActivityCreateWorldBinding
    private val model by viewModels<CreateWorldModel>()
    private lateinit var selectBiome: ActivityResultLauncher<Any?>
    private lateinit var selectOutput: ActivityResultLauncher<Uri?>
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val binding = ActivityCreateWorldBinding.inflate(this.layoutInflater)
        this.binding = binding
        this.setContentView(binding.root)
        binding.toolbar.let {
            this.setSupportActionBar(it)
            it.setNavigationOnClickListener {
                this.finish()
            }
        }
        this.model.biome.observe(this) {
            this.binding.biomeView.biome = it
        }
        binding.biomeView.root.setOnClickListener {
            this@CreateWorldActivity.selectBiome.launch(null)
        }
        this.selectBiome = this.registerForActivityResult(BiomePicker) {
            it?.let { this.model.biome.value = it }
        }
        this.selectOutput = this.registerForActivityResult(FolderPicker) registry@{ path ->
            if (path == null) return@registry
            val activity = this
            this.model.apply {
                layers = activity.binding.fragLayers
                    .getFragment<EditFlatFragment>().resultLayers
                viewModelScope.launch(Dispatchers.IO) {
                    val folder = DocumentFile.fromTreeUri(activity, path) ?: return@launch
                    val config =
                        folder.createFile(MIME_TYPE_DEFAULT, FILE_LEVEL_DAT)
                            ?: return@launch
                    val assets = activity.assets
                    val model = this@apply
                    val data = try {
                        assets.open("dats/1_2_13.dat").use {
                            skip(it, 8)
                            it.readCompound(NbtCompression.None)
                        }
                    } catch (_: Exception) {
                        return@launch
                    }.unwrap().modifyAsCompound {
                        this[KEY_LEVEL_NAME] = model.name.ifBlank {
                            activity.getString(R.string.create_world_name)
                        }.asTag()
                        this[KEY_LAST_PLAYED_TIME] = NbtLong(System.currentTimeMillis() / 1000)
                        var layers = model.layers
                        if (layers.size < 3) {
                            layers = ArrayList<Layer>(layers).also { list ->
                                repeat(3 - layers.size) {
                                    list.add(Layer().apply { amount = 0 })
                                }
                            }
                        }
                        FlatLayers.createNew(
                            biome.value?.id ?: 21,
                            layers
                        ).write()?.let {
                            this[FLAT_WORLD_LAYERS] = it.asTag()
                        }
                    }
                    activity.contentResolver.openOutputStream(config.uri)?.use {
                        NBTOptions(version = 4).write(it, data, "Blocktopograph")
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Done!", Toast.LENGTH_SHORT).show()
                        activity.setResult(RESULT_OK, Intent().setData(folder.uri))
                        activity.finish()
                    }
                }
            }
        }
        binding.fabCreate.setOnClickListener {
            this.model.name = this.binding.name.text.toString()
            this.selectOutput.launch(null)
        }
    }

    override fun applyContentInsets(window: View, insets: Insets) {
        val res = this.resources
        this.binding.fabCreate.applyFloatingInsets(insets) {
            res.getDimensionPixelSize(R.dimen.large_floating_margin)
        }
        val padding = res.getDimensionPixelSize(R.dimen.small_content_padding)
        this.binding.scroll.applyListInsets(
            this.isIndicatorEnabled,
            insets,
            padding,
            padding
        )
    }
}