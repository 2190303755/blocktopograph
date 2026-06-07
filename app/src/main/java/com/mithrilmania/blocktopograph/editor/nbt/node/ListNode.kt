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
import com.mithrilmania.blocktopograph.editor.nbt.InsertionRequest
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.TAG_LIST
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.util.upcoming

class ListNode(
    parent: RootLike,
    key: Any,
    tag: ListTag? = null
) : CollectionNode<NBTNode, ListTag>(parent, key, tag) {
    override val type: Byte get() = TAG_LIST
    override val canBeHeterogeneous: Boolean get() = true
    override fun makePath(child: String): String = "$path[$child]"
    override fun toBinaryTag(): ListTag = ListTag(
        this.children.mapTo(
            mutableListOf(),
            NBTNode::toBinaryTag
        )
    )

    override fun cast(node: NBTNode): NBTNode = node

    override fun buildNodes(
        tag: ListTag
    ): MutableList<NBTNode> = tag.tags.mapIndexedTo(
        mutableStateListOf()
    ) { index, value ->
        value.buildNode(this, index)
    }

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_list)

    @Composable
    override fun summary() = "${this.children.size}个子标签" // TODO i18n

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
                editor.insertion = InsertionRequest(this@ListNode)
            }
        }
    }
}