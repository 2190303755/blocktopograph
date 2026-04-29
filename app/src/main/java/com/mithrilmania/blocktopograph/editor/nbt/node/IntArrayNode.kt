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
import com.mithrilmania.blocktopograph.editor.nbt.Insert
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.TAG_INT_ARRAY
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem

class IntArrayNode(
    parent: RootLike,
    key: Any,
    tag: IntArrayTag? = null
) : CollectionNode<IntNode, IntArrayTag>(parent, key, tag) {
    override val type: Byte get() = TAG_INT_ARRAY
    override fun toBinaryTag(): IntArrayTag {
        val nodes = this.children
        return IntArrayTag(
            IntArray(nodes.size) { nodes[it].value }
        )
    }

    override fun cast(node: NBTNode): IntNode? {
        when (node) {
            is IntNode -> return node
            is ValueNode -> {
                val tag = node.toBinaryTag()
                val value = if (tag is NumericTag) {
                    tag.toInt()
                } else {
                    tag.toString().toIntOrNull() ?: return null
                }
                return IntNode(node.parent, node.key, value)
            }

            else -> return null
        }
    }

    override fun buildNodes(
        tag: IntArrayTag
    ): MutableList<IntNode> = tag.elements.mapIndexedTo(
        mutableListOf()
    ) { index, value ->
        IntNode(this, index, value)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Row(modifier = modifier.padding(start = 4.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_tag_int_array),
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
            editor.performOperation(
                Insert(
                    this,
                    IntNode(
                        this,
                        this.children.size,
                        0
                    )
                )
            )
        }
    }
}