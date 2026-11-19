package com.mithrilmania.blocktopograph.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.world.WorldModel
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.access.WorldAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.function.BiConsumer

fun openDB(
    model: WorldModel,
    owner: LifecycleOwner,
    consumer: BiConsumer<WorldAccess, WorldStorage>
) = owner.lifecycleScope.launch(Dispatchers.IO) {
    model.awaitStorage()?.let {
        withContext(Dispatchers.Main) {
            consumer.accept(model.world, it)
        }
    }
}
