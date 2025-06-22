package com.mithrilmania.blocktopograph.util

import android.content.Context
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.nbt.EditableNBT
import com.mithrilmania.blocktopograph.nbt.EditorFragment
import com.mithrilmania.blocktopograph.nbt.EditorFragment.ChainTag
import com.mithrilmania.blocktopograph.world.WorldHandler
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.unnamed.b.atv.model.TreeNode
import com.unnamed.b.atv.view.AndroidTreeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun openDB(
    handler: WorldHandler,
    context: Context,
    consumer: Consumer<WorldStorage>
) = CoroutineScope(Dispatchers.Default).launch {
    val storage = handler.open(this, context).await() ?: return@launch
    withContext(Dispatchers.Main) { consumer.accept(storage) }
}

fun Fragment.populateTree(
    activity: FragmentActivity,
    tree: AndroidTreeView,
    root: TreeNode,
    nbt: EditableNBT
) {
    val nodes = flow {
        for (tag in nbt.tags) {
            emit(
                TreeNode(ChainTag(null, tag)).setViewHolder(
                    EditorFragment.NBTNodeHolder(
                        nbt,
                        activity
                    )
                )
            )
            delay(50)
        }
    }.flowOn(Dispatchers.Default)
    this.lifecycleScope.launch(Dispatchers.Main) {
        nodes.collect {
            tree.addNode(root, it)
        }
    }
}