package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.TAG_LONG_ARRAY
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.util.upcoming

class LongArrayNode(
    parent: RootLike,
    key: Any,
    tag: LongArrayTag? = null
) : CollectionNode<LongNode, LongArrayTag>(parent, key, tag) {
    override val type: Byte get() = TAG_LONG_ARRAY
    override val canBeHeterogeneous: Boolean get() = false
    override fun makePath(child: String): String = "$path[$child]"
    override fun toBinaryTag(): LongArrayTag {
        val nodes = this.children
        return LongArrayTag(
            LongArray(nodes.size) { nodes[it].value }
        )
    }

    override fun cast(node: NBTNode): LongNode? {
        when (node) {
            is LongNode -> return node
            is ValueNode -> {
                val tag = node.toBinaryTag()
                val value = if (tag is NumericTag) {
                    tag.toLong()
                } else {
                    tag.toString().toLongOrNull() ?: return null
                }
                return LongNode(node.parent, node.key, value)
            }

            else -> return null
        }
    }

    override fun buildNodes(
        tag: LongArrayTag
    ): MutableList<LongNode> = tag.elements.mapIndexedTo(
        mutableStateListOf()
    ) { index, value ->
        LongNode(this, index, value)
    }

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_int_array) // TODO long array icon

    @Composable
    override fun summary() = "${this.children.size}个元素" // TODO i18n

    @Composable
    override fun Editor(editor: NBTEditorModel) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                        this@LongArrayNode,
                        LongNode(
                            this@LongArrayNode,
                            this@LongArrayNode.children.size,
                            0L
                        )
                    )
                )
            }
        }
    }
}