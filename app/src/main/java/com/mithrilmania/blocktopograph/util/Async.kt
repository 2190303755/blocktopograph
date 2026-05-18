package com.mithrilmania.blocktopograph.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.function.Consumer

fun openDB(
    model: WorldModel,
    owner: LifecycleOwner,
    consumer: Consumer<World>
) = owner.lifecycleScope.launch(Dispatchers.IO) {
    model.storage.await()
    withContext(Dispatchers.Main) {
        consumer.accept(model.world)
    }
}
