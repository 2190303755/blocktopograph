package com.mithrilmania.blocktopograph.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.ui.AppNavKey.WorldList

class SharedViewModel(key: AppNavKey? = null) : ViewModel() {
    val backStack = mutableStateListOf(key ?: WorldList)
    val snackbar = SnackbarHostState()
}
