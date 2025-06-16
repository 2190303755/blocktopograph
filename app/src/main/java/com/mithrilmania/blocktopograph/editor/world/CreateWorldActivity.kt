package com.mithrilmania.blocktopograph.editor.world

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityCreateWorldBinding
import com.mithrilmania.blocktopograph.flat.EditFlatFragment
import com.mithrilmania.blocktopograph.flat.FlatLayers
import com.mithrilmania.blocktopograph.flat.Layer
import com.mithrilmania.blocktopograph.nbt.Keys.FLAT_WORLD_LAYERS
import com.mithrilmania.blocktopograph.nbt.asTag
import com.mithrilmania.blocktopograph.nbt.convert.LevelDataConverter.skip
import com.mithrilmania.blocktopograph.nbt.modifyAsCompound
import com.mithrilmania.blocktopograph.nbt.readCompound
import com.mithrilmania.blocktopograph.nbt.unwrapAfterRead
import com.mithrilmania.blocktopograph.nbt.wrapBeforeSave
import com.mithrilmania.blocktopograph.util.BYTE_0
import com.mithrilmania.blocktopograph.util.BiomePicker
import com.mithrilmania.blocktopograph.util.FolderPicker
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_TIME
import com.mithrilmania.blocktopograph.world.KEY_LEVEL_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtLong
import net.benwoodworth.knbt.NbtVariant


class CreateWorldActivity : BaseActivity() {
    private lateinit var binding: ActivityCreateWorldBinding
    private lateinit var model: CreateWorldModel
    private lateinit var selectBiome: ActivityResultLauncher<Any?>
    private lateinit var selectOutput: ActivityResultLauncher<Uri?>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.model = ViewModelProvider(this)[CreateWorldModel::class]
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
        this.selectOutput = this.registerForActivityResult(FolderPicker) { path ->
            if (path == null) return@registerForActivityResult
            val folder = DocumentFile.fromTreeUri(this@CreateWorldActivity, path)
                ?: return@registerForActivityResult
            val config = folder.createFile("application/octet-stream", FILE_LEVEL_DAT)
                ?: return@registerForActivityResult
            this.model.apply {
                layers = this@CreateWorldActivity.binding.fragLayers
                    .getFragment<EditFlatFragment>().resultLayers
                viewModelScope.launch(Dispatchers.IO) {
                    val assets = this@CreateWorldActivity.assets
                    val model = this@apply
                    val data = try {
                        assets.open("dats/1_2_13.dat").use {
                            skip(it, 8)
                            it.readCompound(NbtCompression.None)
                        }
                    } catch (_: Exception) {
                        return@launch
                    }.unwrapAfterRead().modifyAsCompound {
                        this[KEY_LEVEL_NAME] = model.name.ifBlank {
                            this@CreateWorldActivity.getString(R.string.create_world_name)
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
                    this@CreateWorldActivity.contentResolver.openOutputStream(config.uri)?.use {
                        it.write(Nbt {
                            variant = NbtVariant.Bedrock
                            compression = NbtCompression.None
                        }.encodeToByteArray(data.wrapBeforeSave()).let { nbt ->
                            byteArrayOf(
                                4,
                                BYTE_0,
                                BYTE_0,
                                BYTE_0,
                                (nbt.size and 255).toByte(),
                                (nbt.size ushr 8 and 255).toByte(),
                                (nbt.size ushr 16 and 255).toByte(),
                                (nbt.size ushr 24 and 255).toByte()
                            ).plus(nbt)
                        })
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateWorldActivity, "Done!", Toast.LENGTH_SHORT).show()
                        this@CreateWorldActivity.finish()
                    }
                }
            }
        }
        binding.fabCreate.setOnClickListener {
            this.model.name = this.binding.name.text.toString()
            this.selectOutput.launch(null)
        }
    }

    override fun updateDecorViewPadding(decorView: View, systemBars: Insets, ime: Insets) {
        super.updateDecorViewPadding(decorView, systemBars, ime)
        this.binding.fabCreate.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin =
                systemBars.bottom + resources.getDimensionPixelSize(R.dimen.medium_floating_margin)
        }
    }
}