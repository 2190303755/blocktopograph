package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.Insert
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.TAG_INT_ARRAY
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.util.upcoming

class IntArrayNode(
    parent: RootLike,
    key: Any,
    tag: IntArrayTag? = null
) : CollectionNode<IntNode, IntArrayTag>(parent, key, tag) {
    override val type: Byte get() = TAG_INT_ARRAY
    override val canBeHeterogeneous: Boolean get() = false
    override fun makePath(child: String): String = "$path[$child]"
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
        mutableStateListOf()
    ) { index, value ->
        IntNode(this, index, value)
    }

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_int_array)

    @Composable
    override fun summary() = "${this.children.size}个元素" // TODO i18n

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
                editor.performOperation(
                    Insert(
                        this@IntArrayNode,
                        IntNode(
                            this@IntArrayNode,
                            this@IntArrayNode.children.size,
                            0
                        )
                    )
                )
            }
        }
    }
}