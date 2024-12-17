package com.mithrilmania.blocktopograph.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * All callers should be rewritten in kotlin
 */
fun run(action: Runnable) {
    CoroutineScope(Dispatchers.Default).launch {
        action.run()
    }
}