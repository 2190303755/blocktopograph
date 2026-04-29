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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.TAG_FLOAT

class FloatNode(
    parent: RootLike,
    key: Any,
    value: Float
) : ValueNode(parent, key) {
    var value: Float by mutableFloatStateOf(value)
    override val type: Byte get() = TAG_FLOAT
    override fun toBinaryTag(): FloatTag = FloatTag(this.value)

    @Composable
    override fun Content(modifier: Modifier) {
        var textValue by remember(value) { mutableStateOf(value.toString()) }
        Row(modifier = modifier.padding(start = 4.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_tag_float),
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
                    newValue.toFloatOrNull()?.let { floatValue ->
                        value = floatValue
                    }
                },
                modifier = Modifier.width(200.dp),
                singleLine = true
            )
        }
    }
}