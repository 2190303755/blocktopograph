package com.mithrilmania.blocktopograph.editor.nbt

import android.net.Uri
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.DialogNbtExportBinding
import com.mithrilmania.blocktopograph.nbt.NBTConfig
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant


internal class NBTExportDialog(
    activity: NBTEditorActivity,
    title: String,
    config: NBTConfig?,
    action: (Uri?) -> Unit
) : BottomSheetDialog(activity) {
    private val binding = DialogNbtExportBinding.inflate(activity.layoutInflater)

    init {
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        window?.let {
            it.decorView.setOnApplyWindowInsetsListener { view, insets ->
                /*insets.also {
                    val base = activity.systemBarsInsets.value!!.bottom
                    binding.root.updatePadding(bottom = base)
                    view.updatePadding(bottom = -base)
                }*/
                insets
            }
        }
        binding.title.text = title
        if (config == null) {
            binding.options.visibility = View.GONE
        } else {
            fun isCompleted() {
                config.isCompleted().let {
                    binding.buttonPositive.isEnabled = it
                    binding.header.isEnabled = it && config.mayHasHeader()
                }
            }
            config.stringify.observe(activity) {
                val temp = it == true
                binding.endianLittle.isEnabled = !temp
                binding.endianBig.isEnabled = !temp
                binding.compressionNone.isEnabled = !temp
                binding.compressionGzip.isEnabled = !temp
                binding.compressionZlib.isEnabled = !temp
                binding.format.isEnabled = temp
                isCompleted()
            }
            config.variant.observe(activity) { isCompleted() }
            config.compression.observe(activity) { isCompleted() }
            binding.type.addOnButtonCheckedListener { group, _, _ ->
                when (group.checkedButtonId) {
                    R.id.type_nbt -> config.stringify.value = false
                    R.id.type_snbt -> config.stringify.value = true
                }
            }
            binding.endian.addOnButtonCheckedListener { group, _, _ ->
                when (group.checkedButtonId) {
                    R.id.endian_little -> config.variant.value = NbtVariant.Bedrock
                    R.id.endian_big -> config.variant.value = NbtVariant.Java
                }
            }
            binding.compression.addOnButtonCheckedListener { group, _, _ ->
                when (group.checkedButtonId) {
                    R.id.compression_none -> config.compression.value = NbtCompression.None
                    R.id.compression_gzip -> config.compression.value = NbtCompression.Gzip
                    R.id.compression_zlib -> config.compression.value = NbtCompression.Zlib
                }
            }
            binding.header.setOnCheckedChangeListener { _, value -> config.header = value }
            binding.format.setOnCheckedChangeListener { _, value -> config.format = value }
            when (config.stringify.value) {
                true -> binding.type.check(R.id.type_snbt)
                else -> {
                    binding.type.check(R.id.type_nbt)
                    when (config.variant.value) {
                        NbtVariant.Bedrock -> binding.endian.check(R.id.endian_little)
                        NbtVariant.Java -> binding.endian.check(R.id.endian_big)
                    }
                    when (config.compression.value) {
                        NbtCompression.None -> binding.compression.check(R.id.compression_none)
                        NbtCompression.Gzip -> binding.compression.check(R.id.compression_gzip)
                        NbtCompression.Zlib -> binding.compression.check(R.id.compression_zlib)
                    }
                    binding.header.isChecked = config.header == true
                }
            }
        }
        setContentView(binding.root)
    }
}