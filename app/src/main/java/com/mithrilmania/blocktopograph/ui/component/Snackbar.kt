package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

suspend inline fun SnackbarHostState.showSnackbar(
    message: String,
    actionLabel: String,
    duration: SnackbarDuration = SnackbarDuration.Short,
    withDismissAction: Boolean = false,
    onConfirm: () -> Unit
) {
    if (SnackbarResult.ActionPerformed ==
        this.showSnackbar(message, actionLabel, withDismissAction, duration)
    ) {
        onConfirm()
    }
}