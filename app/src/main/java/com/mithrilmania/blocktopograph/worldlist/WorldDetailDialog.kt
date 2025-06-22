package com.mithrilmania.blocktopograph.worldlist

import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textview.MaterialTextView
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.DialogWorldDetailBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorActivity
import com.mithrilmania.blocktopograph.editor.world.WorldEditorActivity
import com.mithrilmania.blocktopograph.test.WorldTestActivity
import com.mithrilmania.blocktopograph.util.clipboard
import com.mithrilmania.blocktopograph.util.makeCommonDialog
import com.mithrilmania.blocktopograph.util.toast
import java.util.Date

class WorldDetailDialog : BottomSheetDialogFragment() {
    private var binding: DialogWorldDetailBinding? = null
    val model by activityViewModels<WorldListModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?) = this.makeCommonDialog()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.binding?.apply {
            path.copyOnClick()
            seed.copyOnClick()
        }
        this.model.adapter.selected.observe(this) { world ->
            val binding = this.binding ?: return@observe
            val context = this.requireContext()
            if (world === null) return@observe
            binding.name.text = world.name
            binding.icon.apply {
                if (world.icon == null) {
                    setImageResource(R.drawable.world_icon_default)
                } else {
                    setImageBitmap(world.icon)
                }
                var lastClick = 0L
                var clickCount = 0L
                setOnClickListener {
                    val current = System.currentTimeMillis()
                    if (current - lastClick < 500) {
                        if (++clickCount > 4) {
                            clickCount = 0
                            context.startActivity(
                                world.applyTo(Intent(context, WorldTestActivity::class.java))
                            )
                        }
                    } else {
                        clickCount = 1
                    }
                    lastClick = current
                }
            }
            binding.path.text = world.path
            binding.mode.text = world.mode
            DateFormat.getTimeFormat(context)
            binding.date.text = Date(world.time).let {
                "${
                    DateFormat.getMediumDateFormat(context).format(it)
                } ${
                    DateFormat.getTimeFormat(context).format(it)
                } - ${world.version}"
            }
            binding.size.text = world.size ?: context.getString(R.string.calculating_size)
            binding.seed.text = world.seed
            binding.editWorld.setOnClickListener {
                context.startActivity(
                    world.applyTo(Intent(context, WorldEditorActivity::class.java))
                )
            }
            binding.editConfig.setOnClickListener {
                context.startActivity(
                    world.config.applyTo(
                        Intent(
                            context,
                            NBTEditorActivity::class.java
                        ).setAction(Intent.ACTION_VIEW)
                    )
                )
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (this.model.adapter.selected.value === null) {
            this.dismiss()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        this.model.adapter.selected.value = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = DialogWorldDetailBinding.inflate(inflater, container, false).also {
        this.binding = it
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        this.binding = null
    }

    companion object {
        const val TAG = "WorldDetailDialog"
        fun MaterialTextView.copyOnClick() {
            (this.parent?.parent?.parent as? View)?.setOnClickListener {
                if (this.text.isNullOrBlank()) return@setOnClickListener
                this.context.clipboard?.setPrimaryClip(ClipData.newPlainText("", this.text))
                    ?: return@setOnClickListener
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    this.context.toast(R.string.toast_copy_success)
                }
            }
        }
    }
}