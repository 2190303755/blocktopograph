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
import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE_ARRAY
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.util.upcoming

class ByteArrayNode(
    parent: RootLike,
    key: Any,
    tag: ByteArrayTag? = null
) : CollectionNode<ByteNode, ByteArrayTag>(parent, key, tag) {
    override val type: Byte get() = TAG_BYTE_ARRAY
    override val canBeHeterogeneous: Boolean get() = false
    override fun makePath(child: String): String = "$path[$child]"
    override fun toBinaryTag(): ByteArrayTag {
        val nodes = this.children
        return ByteArrayTag(
            ByteArray(nodes.size) { nodes[it].value }
        )
    }

    override fun cast(node: NBTNode): ByteNode? {
        when (node) {
            is ByteNode -> return node
            is ValueNode -> {
                val tag = node.toBinaryTag()
                val value = if (tag is NumericTag) {
                    tag.toByte()
                } else {
                    tag.toString().toByteOrNull() ?: return null
                }
                return ByteNode(node.parent, node.key, value)
            }

            else -> return null
        }
    }

    override fun buildNodes(
        tag: ByteArrayTag
    ): MutableList<ByteNode> = tag.elements.mapIndexedTo(
        mutableStateListOf()
    ) { index, value ->
        ByteNode(this, index, value)
    }

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_byte_array)

    @Composable
    override fun summary() = "${this.children.size}个元素"// TODO i18n

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
                        this@ByteArrayNode,
                        ByteNode(
                            this@ByteArrayNode,
                            this@ByteArrayNode.children.size,
                            0.toByte()
                        )
                    )
                )
            }
        }
    }
}