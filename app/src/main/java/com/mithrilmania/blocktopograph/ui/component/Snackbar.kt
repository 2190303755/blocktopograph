package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

suspend inline fun SnackbarHostState.awaitSnackbar(
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

@Composable
fun SwipeableSnackbar(
    data: SnackbarData,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) }
) {
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(),
        backgroundContent = { },
        onDismiss = { data.dismiss() },
    ) {
        snackbar(data)
    }
}
