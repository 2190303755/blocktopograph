package com.mithrilmania.blocktopograph.ui.screen.nbt.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.TAG_INT_ARRAY
import com.mithrilmania.blocktopograph.nbt.toIntList
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.ui.component.safeLayoutInsets
import com.mithrilmania.blocktopograph.ui.screen.nbt.Insert
import com.mithrilmania.blocktopograph.ui.screen.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.util.upcoming

class IntArrayNode(
    parent: RootLike,
    key: Any,
    tag: IntArrayTag? = null
) : CollectionNode<IntNode, IntArrayTag>(parent, key, tag) {
    override val type: Byte get() = TAG_INT_ARRAY
    override val canBeHeterogeneous: Boolean get() = false
    override fun makePath(child: String): String = "$path[$child]"
    override fun toBinaryTag(): IntArrayTag = IntArrayTag(
        this.children.toIntList { it.value }
    )

    override fun cast(node: NBTNode): IntNode? {
        when (node) {
            is IntNode -> return node
            is ValueNode -> {
                val tag = node.toBinaryTag()
                val value = if (tag is NumericTag) {
                    tag.toInt()
                } else {
                    tag.toString().trim().toIntOrNull() ?: return null
                }
                return IntNode(node.parent, node.key, value)
            }

            else -> return null
        }
    }

    override fun buildNodes(
        tag: IntArrayTag
    ): MutableList<IntNode> {
        val nodes = mutableStateListOf<IntNode>()
        tag.elements.forEachIndexed { index, element ->
            nodes.add(IntNode(this, index, element))
        }
        return nodes
    }

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_int_array)

    @Composable
    override fun summary() = "${this.children.size}个元素".todoI18n()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Editor(editor: NBTEditorModel, sheetState: SheetState) {
        Row(
            modifier = Modifier.windowInsetsPadding(
                safeLayoutInsets()
                    .only(WindowInsetsSides.Bottom)
                    .union(WindowInsets.ime)
            ), horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val context = LocalContext.current
            BottomSheetActionButton(
                icon = Icons.Filled.Search,
                text = "查找".todoI18n(),
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
                            children.size,
                            0
                        )
                    )
                )
            }
        }
    }
}