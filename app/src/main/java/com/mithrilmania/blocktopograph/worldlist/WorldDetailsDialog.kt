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
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.WorldActivity
import com.mithrilmania.blocktopograph.databinding.DialogWorldDetailsBinding
import com.mithrilmania.blocktopograph.nbt.tags.LongTag
import com.mithrilmania.blocktopograph.world.KEY_RANDOM_SEED
import java.util.Date

class WorldDetailsDialog(
    activity: WorldListActivity,
    val model: WorldListModel
) : BottomSheetDialog(activity), DialogInterface.OnCancelListener,
    View.OnApplyWindowInsetsListener {
    val binding: DialogWorldDetailsBinding

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
        this.model.selected.observe(activity) { world ->
            if (world == null) return@observe
            binding.name.text = world.name
            if (world.icon == null) {
                binding.icon.setImageResource(R.drawable.world_icon_default)
            } else {
                binding.icon.setImageBitmap(world.icon)
            }
            binding.path.text = world.location.lastPathSegment
            binding.mode.text = world.mode
            binding.date.text =
                DateFormat.getDateFormat(activity).format(Date(world.time))
            binding.size.text = world.size ?: activity.getString(R.string.calculating_size)
            binding.seed.text =
                (world.config?.getChildTagByKey(KEY_RANDOM_SEED) as? LongTag)?.value?.toString()
            binding.editWorld.setOnClickListener {
                activity.startActivity(
                    Intent(
                        activity,
                        WorldActivity::class.java
                    ).setData(world.location)
                )
            }
            binding.editConfig.setOnClickListener {
                Toast.makeText(activity, "前面的区域，以后再来探索吧！", Toast.LENGTH_SHORT).show()
            }
            this.show()
        }
        bindDetail(binding.path)
        bindDetail(binding.mode)
        bindDetail(binding.date)
        bindDetail(binding.size)
        bindDetail(binding.seed)
        this.setContentView(binding.root)
        this.dismissWithAnimation = true
        this.setOnCancelListener(this)
        behavior.skipCollapsed = true

    }

    override fun onAttachedToWindow() {
        val window = this.window
        if (window != null) @Suppress("DEPRECATION") window.navigationBarColor =
            0x00FFFFFF // to enable edge to edge
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
        window.decorView.setOnApplyWindowInsetsListener(this)
    }

    override fun onCancel(dialog: DialogInterface?) {
        this.model.selected.value = null
    }

    override fun show() {
        super.show()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsets): WindowInsets {
        /*val isIndicatorUsed =
            when (Settings.Secure.getInt(this.context.contentResolver, "navigation_mode", 0)) {
                INDICATOR_NAV_MODE_ANDROID, INDICATOR_NAV_MODE_HARMONY -> true
                else -> false
            }*/
        val systemBarsInsets = WindowInsetsCompat
            .toWindowInsetsCompat(insets)
            .getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            top = 0,
            bottom = 0,
            left = systemBarsInsets.left,
            right = systemBarsInsets.right
        )
        return insets
    }
}