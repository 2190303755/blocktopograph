package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.editor.nbt.Operation
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.TAG_STRING
import com.mithrilmania.blocktopograph.nbt.util.appendQuoted
import com.mithrilmania.blocktopograph.ui.component.IconButton

class StringNode(
    parent: RootLike,
    key: Any,
    value: String
) : ValueNode(parent, key) {
    var value: String by mutableStateOf(value)
    override val type: Byte get() = TAG_STRING
    override fun toBinaryTag(): StringTag = StringTag(this.value)

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_string)

    @Composable
    override fun summary() = StringBuilder().appendQuoted(value).toString()

    @Composable
    override fun Editor(editor: NBTEditorModel) {
        val focusRequester = remember { FocusRequester() }
        val textFieldState = rememberTextFieldState(value)
        OutlinedTextField(
            state = textFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            trailingIcon = {
                IconButton(Icons.Filled.Check) {
                    editor.performOperation(
                        Assign(textFieldState.text.toString())
                    )
                }
            },
            supportingText = {
                Spacer(Modifier.height(with(LocalDensity.current) {
                    LocalTextStyle.current.lineHeight.toDp()
                }))
            }
        )
        LaunchedEffect(value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }

    inner class Assign(val neo: String) : Operation {
        val old: String = this@StringNode.value
        override fun redo(editor: NBTEditorModel) {
            this@StringNode.value = this.neo
        }

        override fun undo(editor: NBTEditorModel) {
            this@StringNode.value = this.old
        }
    }
}