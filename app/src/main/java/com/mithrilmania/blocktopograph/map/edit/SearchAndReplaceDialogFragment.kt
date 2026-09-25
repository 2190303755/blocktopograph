package com.mithrilmania.blocktopograph.map.edit

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.selection.SelectionMenuFragment
import com.mithrilmania.blocktopograph.ui.component.DialogFragmentLayout
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme
import kotlinx.coroutines.launch

class SearchAndReplaceDialogFragment @JvmOverloads constructor(
    var entry: SelectionMenuFragment.EditFunctionEntry? = null
) : DialogFragment() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ComposeView {
        val root = ComposeView(this.requireContext())
        // Dispose of the Composition when the view's LifecycleOwner is destroyed
        root.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        root.setContent {
            BlocktopographCompatTheme {
                val model = viewModel<SearchAndReplaceModel>()
                DialogFragmentLayout(
                    title = stringResource(R.string.map_edit_func_snr),
                    modifier = Modifier
                        .clip(AlertDialogDefaults.shape)
                        .background(MaterialTheme.colorScheme.surface),
                    buttonsArrangement = Arrangement.SpaceBetween,
                    buttons = {
                        val tooltipState = rememberTooltipState()
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = {
                                PlainTooltip { Text(stringResource(R.string.map_edit_snr_help_background)) }
                            },
                            state = tooltipState
                        ) {
                            val scope = rememberCoroutineScope()
                            TextButton(stringResource(R.string.action_help)) {
                                scope.launch { tooltipState.show(MutatePriority.UserInput) }
                            }
                        }
                        TextButton(stringResource(android.R.string.ok)) {
                            val bundle = Bundle()
                            bundle.putSerializable(CONFIG, model.buildConfig())
                            entry?.invokeEditFunction(EditFunction.SNR, bundle)
                            dismiss()
                        }
                    }
                ) {
                    SearchAndReplaceLayout(model)
                }
            }
        }
        return root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return ComponentDialog(this.requireContext(), R.style.ComposeDialog)
    }

    companion object {
        const val CONFIG = "config"
    }
}