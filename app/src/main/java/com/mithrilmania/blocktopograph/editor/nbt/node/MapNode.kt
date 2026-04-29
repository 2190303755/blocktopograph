package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.InsertionRequest
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem
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
    override val children: Collection<NBTNode> get() = this.nodes.values
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
    override fun Content(modifier: Modifier) {
        Row(modifier = modifier.padding(start = 4.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_tag_compound),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = key.toString()
            )
        }
    }

    @Composable
    override fun ContextMenu(editor: NBTEditorModel) {
        DropdownMenuItem(
            Icons.Filled.Add,
            stringResource(R.string.action_insert)
        ) {
            this.showContextMenu = false
            editor.insertion = InsertionRequest(this)
        }
    }
}