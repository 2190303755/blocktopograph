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
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.TAG_LIST
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem

class ListNode(
    parent: RootLike,
    key: Any,
    tag: ListTag? = null
) : CollectionNode<NBTNode, ListTag>(parent, key, tag) {
    override val type: Byte get() = TAG_LIST
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
        mutableListOf()
    ) { index, value ->
        value.buildNode(this, index)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Row(modifier = modifier.padding(start = 4.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_tag_list),
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