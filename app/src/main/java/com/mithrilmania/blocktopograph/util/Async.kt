package com.mithrilmania.blocktopograph.util

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.world.WorldHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.function.Consumer

fun openDB(
    handler: WorldHandler,
    owner: LifecycleOwner,
    context: Context,
    consumer: Consumer<WorldHandler>
) = owner.lifecycleScope.launch(Dispatchers.IO) {
    handler.open(context) ?: return@launch
    withContext(Dispatchers.Main) { consumer.accept(handler) }
}
