package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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

fun SnackbarHostState.showSnackbar(
    fragment: Fragment,
    message: String,
    duration: SnackbarDuration
) {
    fragment.lifecycleScope.launch {
        showSnackbar(message, duration = duration)
    }
}
