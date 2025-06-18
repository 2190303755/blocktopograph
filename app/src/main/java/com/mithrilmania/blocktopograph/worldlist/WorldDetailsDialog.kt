package com.mithrilmania.blocktopograph.worldlist

import android.content.ClipData
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.WorldActivity
import com.mithrilmania.blocktopograph.databinding.DialogWorldDetailsBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorActivity
import com.mithrilmania.blocktopograph.test.WorldTestActivity
import com.mithrilmania.blocktopograph.util.clipboard
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.world.WorldInfo
import java.util.Date

class WorldDetailsDialog(
    activity: WorldListActivity,
    val model: WorldListModel
) : BottomSheetDialog(activity, 0), DialogInterface.OnCancelListener {
    val binding: DialogWorldDetailsBinding
    val callback = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                ViewCompat.offsetTopAndBottom(
                    bottomSheet,
                    behavior.expandedOffset - bottomSheet.top
                )
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    private fun bindDetail(view: MaterialTextView) {
        (view.parent?.parent?.parent as? View)?.setOnClickListener {
            if (view.text.isNullOrBlank()) return@setOnClickListener
            this.context.clipboard?.setPrimaryClip(ClipData.newPlainText("", view.text))
                ?: return@setOnClickListener
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                this.context.toast(R.string.toast_copy_success)
            }
        }
    }

    init {
        val binding = DialogWorldDetailsBinding.inflate(activity.layoutInflater)
        this.binding = binding
        this.model.adapter.selected.observe(activity) {
            this.bindWorld(it ?: return@observe)
            this.show()
        }
        bindDetail(binding.path)
        bindDetail(binding.seed)
        this.setContentView(binding.root)
        this.dismissWithAnimation = true
        this.setOnCancelListener(this)
        behavior.apply {
            addBottomSheetCallback(callback)
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    fun bindWorld(world: WorldInfo) {
        val binding = this.binding
        val context = this.context
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
                world.applyTo(Intent(context, WorldActivity::class.java))
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

    override fun onAttachedToWindow() {
        val window = this.window
        if (window != null) {
            // to enable edge to edge
            @Suppress("DEPRECATION")
            window.navigationBarColor = 0x00FFFFFF
        }
        super.onAttachedToWindow()
        if (window == null) return
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        window.isNavigationBarContrastEnforced = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        window.attributes.apply {
            blurBehindRadius = 20
            dimAmount = 0.16F
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    }

    override fun onCancel(dialog: DialogInterface?) {
        this.model.adapter.selected.value = null
    }
}