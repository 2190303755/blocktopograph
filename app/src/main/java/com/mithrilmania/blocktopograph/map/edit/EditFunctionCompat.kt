package com.mithrilmania.blocktopograph.map.edit

import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.map.Biome
import com.mithrilmania.blocktopograph.map.MapFragment
import com.mithrilmania.blocktopograph.map.edit.SnrConfig.SearchConditionBlock
import com.mithrilmania.blocktopograph.util.UiUtil
import com.mithrilmania.blocktopograph.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun SnrConfig.perform(target: EditTarget): EditResultCode {
    target.setMaxError(Int.MAX_VALUE)
    target.forEachXyz(SnrEdit(this))
    return EditResultCode.SUCCESS
}

fun performSelectionBasedEdit(
    fragment: MapFragment,
    area: RectEditTarget,
    action: EditFunction,
    args: Bundle?
) {
    var job: Job? = null
    val dialog = UiUtil.buildProgressWaitDialog(
        fragment.requireContext(),
        R.string.general_please_wait
    ) { job?.cancel() }
    // TODO consider viewModelScope
    job = fragment.lifecycleScope.launch(Dispatchers.Default) {
        val code = when (action) {
            EditFunction.LAMPSHADE -> {
                val cfg = SnrConfig()
                cfg.searchMode = 2
                cfg.placeMode = 1
                cfg.searchBlockMain = SearchConditionBlock(
                    BlockTemplates.getOfType("minecraft:torch")[0].block,
                    true,
                    true
                )
                cfg.placeOldBlockMain = BlockTemplates.getOfType("minecraft:glass")[0].block
                cfg.ignoreSubId = true
                cfg.perform(area)
            }

            EditFunction.SNR -> if (args === null) {
                EditResultCode.GENERAL_FAILURE
            } else {
                BundleCompat.getSerializable(
                    args,
                    SearchAndReplaceDialogFragment.CONFIG,
                    SnrConfig::class.java
                )?.perform(area) ?: EditResultCode.GENERAL_FAILURE
            }

            EditFunction.DCHUNK -> {
                area.setMaxError(Int.MAX_VALUE)
                area.forEachChunk(DchunkEdit())
                EditResultCode.SUCCESS
            }

            EditFunction.CHBIOME -> if (args === null) {
                EditResultCode.GENERAL_FAILURE
            } else {
                val to = BundleCompat.getSerializable(
                    args,
                    ChBiomeFragment.KEY_TO,
                    Biome::class.java
                )
                if (to === null) {
                    EditResultCode.GENERAL_FAILURE
                } else {
                    area.setMaxError(Int.MAX_VALUE)
                    area.forEachXz(
                        ChBiomeEdit(
                            BundleCompat.getSerializable(
                                args,
                                ChBiomeFragment.KEY_FROM,
                                Biome::class.java
                            ), to
                        )
                    )
                    EditResultCode.SUCCESS
                }
            }

            else -> EditResultCode.GENERAL_FAILURE
        }
        withContext(Dispatchers.Main) {
            dialog.dismiss()
            if (code === EditResultCode.SUCCESS) {
                dialog.context.toast(R.string.general_done)
                fragment.refreshAfterEdit()
            } else {
                dialog.context.toast(R.string.general_failed)
            }
        }
    }
    dialog.show()
}

