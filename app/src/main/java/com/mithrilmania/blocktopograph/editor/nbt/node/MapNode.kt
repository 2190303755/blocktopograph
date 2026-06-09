package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.InsertionRequest
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.util.upcoming
import java.util.TreeMap

class MapNode(
    parent: RootLike,
    key: Any,
    tags: Map<String, BinaryTag> = emptyMap()
) : RootNode(parent, key) {
    val nodes: TreeMap<String, NBTNode>

    init {
        val nodes = TreeMap<String, NBTNode>()
        tags.forEach { (name, tag) ->
            nodes[name] = tag.buildNode(this, name)
        }
        this.nodes = nodes
    }

    override val type: Byte get() = TAG_COMPOUND
    override val canBeHeterogeneous: Boolean get() = true
    override val children: Collection<NBTNode> get() = this.nodes.values
    override fun makePath(child: String): String {
        val builder = StringBuilder(this.path)
        if (builder.isNotEmpty()) {
            builder.append('.')
        }
        return builder.appendSafeLiteral(child).toString()
    }

    override fun toBinaryTag(): CompoundTag = CompoundTag(
        this.nodes.mapValuesTo(HashMap()) { it.value.toBinaryTag() }
    )

    override fun remove(key: Any): NBTNode? {
        return this.nodes.remove(key.toString())
    }

    override fun insert(key: Any, node: NBTNode): Boolean {
        val name = key.toString()
        if (this.nodes.containsKey(name)) return false
        this.nodes[name] = node
        return true
    }

    override fun replace(key: Any, node: NBTNode): Boolean {
        val name = key.toString()
        if (this.nodes.containsKey(name)) {
            this.nodes[name] = node
            return true
        }
        return false
    }

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_compound)

    @Composable
    override fun summary() = "${this.children.size}个键值对" // TODO i18n

    @Composable
    override fun Editor(editor: NBTEditorModel) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val context = LocalContext.current
            BottomSheetActionButton(
                icon = Icons.Filled.Search,
                text = "查找", // TODO i18n
                modifier = Modifier.weight(1.0F)
            ) {
                context.upcoming()
            }
            BottomSheetActionButton(
                icon = Icons.Filled.Add,
                text = stringResource(R.string.action_insert),
                modifier = Modifier.weight(1.0F)
            ) {
                editor.insertion = InsertionRequest(this@MapNode)
            }
        }
    }
}