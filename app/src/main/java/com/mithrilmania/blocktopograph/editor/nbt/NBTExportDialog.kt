package com.mithrilmania.blocktopograph.editor.nbt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mithrilmania.blocktopograph.EXTRA_INVALIDATED
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.DialogNbtExportBinding
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.makeCommonDialog
import net.benwoodworth.knbt.NbtCompression

class NBTExportDialog : BottomSheetDialogFragment() {
    private lateinit var picker: ActivityResultLauncher<FileCreator.Options?>
    private var binding: DialogNbtExportBinding? = null
    val editor by activityViewModels<NBTEditorModel>()
    val exporter by viewModels<NBTExportModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?) = this.makeCommonDialog()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = this.binding ?: return
        val activity = this.requireActivity()
        val exporter = this.exporter
        when (exporter.stringify.value) {
            true -> binding.type.check(R.id.type_snbt)
            else -> {
                binding.header.isChecked = exporter.header == true
                binding.type.check(R.id.type_nbt)
                if (exporter.littleEndian) {
                    binding.endian.check(R.id.endian_little)
                } else {
                    binding.endian.check(R.id.endian_big)
                }
                binding.compression.check(
                    when (exporter.compression) {
                        NbtCompression.Gzip -> R.id.compression_gzip
                        NbtCompression.Zlib -> R.id.compression_zlib
                        else -> R.id.compression_none
                    }
                )
            }
        }
        binding.export.setOnClickListener {
            val exporter = this.exporter
            val output = exporter.output
            if (output === null) {
                this.picker.launch(exporter.buildOptions())
            } else {
                exporter.saveTo(output, it.context, this.editor)
                this.dismiss()
            }
        }
        exporter.stringify.observe(activity) {
            val binding = this.binding ?: return@observe
            val flag = it == false
            binding.endian.isEnabled = flag
            binding.compression.isEnabled = flag
            binding.prettify.isEnabled = !flag
            binding.export.isEnabled = it !== null
            binding.header.isEnabled = this.exporter.isHeaderAvailable()
        }
        binding.type.addOnButtonCheckedListener { group, _, _ ->
            exporter.stringify.value = group.checkedButtonId != R.id.type_nbt
        }
        binding.endian.addOnButtonCheckedListener click@{ group, _, _ ->
            this.exporter.littleEndian = group.checkedButtonId != R.id.endian_big
            (this.binding ?: return@click).header.isEnabled = this.exporter.isHeaderAvailable()
        }
        binding.compression.addOnButtonCheckedListener click@{ group, _, _ ->
            this.exporter.compression = when (group.checkedButtonId) {
                R.id.compression_none -> NbtCompression.None
                R.id.compression_gzip -> NbtCompression.Gzip
                R.id.compression_zlib -> NbtCompression.Zlib
                else -> return@click
            }
            (this.binding ?: return@click).header.isEnabled = this.exporter.isHeaderAvailable()
        }
        binding.header.apply {
            isEnabled = exporter.isHeaderAvailable()
            setOnCheckedChangeListener { _, value -> exporter.header = value }
        }
        binding.prettify.apply {
            isChecked = exporter.prettify
            setOnCheckedChangeListener { _, value -> exporter.prettify = value }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.picker = registerForActivityResult(FileCreator) registry@{
            this.exporter.saveTo(SAFFile(it ?: return@registry), this.requireContext(), this.editor)
            this.dismiss()
        }
        this.exporter.accept(this.editor, this.requireContext())
        if ((this.arguments ?: return).getBoolean(EXTRA_INVALIDATED)) {
            this.exporter.output = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = DialogNbtExportBinding.inflate(inflater, container, false).also {
        this.binding = it
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        this.binding = null
    }

    companion object {
        const val TAG = "NBTExportDialog"
    }
}