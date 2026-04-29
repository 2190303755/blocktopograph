package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE

class ByteNode(
    parent: RootLike,
    key: Any,
    value: Byte
) : ValueNode(parent, key) {
    var value: Byte by mutableStateOf(value)
    override val type: Byte get() = TAG_BYTE
    override fun toBinaryTag(): ByteTag = ByteTag(this.value)

    @Composable
    override fun Content(modifier: Modifier) {
        var textValue by remember(value) { mutableStateOf(value.toString()) }
        Row(modifier = modifier.padding(start = 4.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_tag_byte),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = key.toString(),
                modifier = Modifier.padding(end = 8.dp)
            )
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    newValue.toByteOrNull()?.let { byteValue ->
                        value = byteValue
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true
            )
        }
    }
}