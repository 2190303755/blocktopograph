package com.mithrilmania.blocktopograph.worldlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.WorldActivity
import com.mithrilmania.blocktopograph.databinding.DialogWorldDetailsBinding
import com.mithrilmania.blocktopograph.world.World
import java.util.Date

class WorldDetailsDialog(
    activity: WorldListActivity,
    val model: WorldListModel
) : BottomSheetDialog(activity), DialogInterface.OnCancelListener {
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
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                ClipData.newPlainText("", view.text)
            )
            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    init {
        val binding = DialogWorldDetailsBinding.inflate(activity.layoutInflater)
        this.binding = binding
        this.model.selected.observe(activity) {
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

    fun bindWorld(world: World<*>) {
        val binding = this.binding
        val context = this.context
        binding.name.text = world.name
        if (world.icon == null) {
            binding.icon.setImageResource(R.drawable.world_icon_default)
        } else {
            binding.icon.setImageBitmap(world.icon)
        }
        binding.path.text = world.path
        binding.mode.text = world.mode
        binding.date.text =
            DateFormat.getDateFormat(context).format(Date(world.time)) + " - " + world.version
        binding.size.text = world.size ?: context.getString(R.string.calculating_size)
        binding.seed.text = world.seed
        binding.editWorld.setOnClickListener {
            context.startActivity(
                world.prepareIntent(Intent(context, WorldActivity::class.java))
            )
        }
        binding.editConfig.setOnClickListener {
            Toast.makeText(context, "前面的区域，以后再来探索吧！", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAttachedToWindow() {
        val window = this.window
        if (window != null) {
            // to enable edge to edge
            @Suppress("DEPRECATION") window.navigationBarColor = 0x00FFFFFF
        }
        super.onAttachedToWindow()
        if (window == null) return
        @Suppress("DEPRECATION") window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.attributes.apply {
                blurBehindRadius = 20
                dimAmount = 0.16F
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        this.model.selected.value = null
    }
}