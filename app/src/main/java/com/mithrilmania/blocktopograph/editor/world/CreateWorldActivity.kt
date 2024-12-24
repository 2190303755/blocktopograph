package com.mithrilmania.blocktopograph.editor.world

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityCreateWorldBinding
import com.mithrilmania.blocktopograph.util.upcoming

class CreateWorldActivity : BaseActivity() {
    private lateinit var binding: ActivityCreateWorldBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCreateWorldBinding.inflate(this.layoutInflater)
        this.setContentView(binding.root)
        binding.toolbar.let {
            this.setSupportActionBar(it)
            it.setNavigationOnClickListener {
                this.finish()
            }
        }
        binding.fabCreate.setOnClickListener {
            doCreate()
        }
        this.binding = binding
    }

    fun doCreate() {
        this.upcoming()
    }

    override fun updateDecorViewPadding(decorView: View, systemBars: Insets, ime: Insets) {
        super.updateDecorViewPadding(decorView, systemBars, ime)
        this.binding.fabCreate.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin =
                systemBars.bottom + resources.getDimension(R.dimen.medium_floating_margin).toInt()
        }
    }
}