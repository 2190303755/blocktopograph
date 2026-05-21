package com.mithrilmania.blocktopograph.editor.nbt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.world.WorldMapModel
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme
import kotlinx.coroutines.launch

class NBTEditorFragment @JvmOverloads constructor(
    val initial: NBTImportModel? = null
) : Fragment() {
    val viewModel by viewModels<NBTEditorModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ComposeView {
        val context = this.requireContext()
        val view = ComposeView(context)
        // Dispose of the Composition when the view's LifecycleOwner is destroyed
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            BlocktopographCompatTheme {
                NBTEditor(this.viewModel) {
                    this.parentFragmentManager.popBackStack()
                }
            }
        }
        if (savedInstanceState === null) {
            this.initial?.let {
                this.viewModel.apply {
                    viewModelScope.launch {
                        readFromFile(it.source, it)
                    }
                }
            }
        }
        return view
    }


    override fun onStart() {
        super.onStart()
        this.activity?.setTitle(R.string.nbt_editor)
    }

    override fun onResume() {
        super.onResume()
        val activity = this.activity
        if (activity !== null) {
            ViewModelProvider(activity)[WorldMapModel::class.java].showActionBar.value = true
        }
    }
}