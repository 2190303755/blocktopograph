package com.mithrilmania.blocktopograph.util

import android.content.Context
import androidx.core.util.Consumer
import com.mithrilmania.blocktopograph.world.WorldHandler
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * All callers should be rewritten in kotlin
 */
fun run(action: Runnable) = CoroutineScope(Dispatchers.Default).launch {
    action.run()
}

fun <T> T.asCompleted() = CompletableDeferred(this)

fun openDB(
    handler: WorldHandler,
    context: Context,
    consumer: Consumer<WorldStorage>
) = CoroutineScope(Dispatchers.Default).launch {
    val storage = handler.open(this, context).await() ?: return@launch
    withContext(Dispatchers.Main) { consumer.accept(storage) }
}