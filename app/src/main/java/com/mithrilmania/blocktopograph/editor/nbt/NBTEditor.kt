package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.DialogRenameNodeBinding

inline fun Context.renameNode(
    name: String,
    crossinline callback: (String) -> Unit
) {
    val binding = DialogRenameNodeBinding.inflate(LayoutInflater.from(this))
    binding.text.setText(name)
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.action_rename)
        .setView(binding.root)
        .setNegativeButton(R.string.option_negative, null)
        .setPositiveButton(R.string.option_positive) { dialog, _ ->
            callback(binding.text.text.toString())
        }.show()
}