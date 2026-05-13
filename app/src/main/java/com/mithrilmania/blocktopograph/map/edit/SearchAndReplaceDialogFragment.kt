package com.mithrilmania.blocktopograph.map.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.selection.SelectionMenuFragment
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme
import kotlinx.coroutines.launch

class SearchAndReplaceDialogFragment @JvmOverloads constructor(
    var entry: SelectionMenuFragment.EditFunctionEntry? = null
) : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ComposeView {
        val view = ComposeView(this.requireContext())
        // Dispose of the Composition when the view's LifecycleOwner is destroyed
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            BlocktopographCompatTheme {
                Column(Modifier.padding(16.dp)) {
                    val model = viewModel<SearchAndReplaceModel>()
                    Text(
                        text = stringResource(R.string.map_edit_func_snr),
                        style = MaterialTheme.typography.titleMedium
                    )
                    SearchAndReplaceLayout(model)
                    Row(Modifier.fillMaxWidth()) {
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
                        Spacer(modifier = Modifier.weight(1.0F))
                        TextButton(stringResource(android.R.string.ok)) {
                            val bundle = Bundle()
                            bundle.putSerializable(CONFIG, model.buildConfig())
                            entry?.invokeEditFunction(EditFunction.SNR, bundle)
                            dismiss()
                        }
                    }
                }
            }
        }
        return view
    }

    companion object {
        const val CONFIG = "config"
    }
}