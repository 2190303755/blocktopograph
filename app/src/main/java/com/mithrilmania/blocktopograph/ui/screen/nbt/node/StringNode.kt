package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.editor.nbt.Operation
import com.mithrilmania.blocktopograph.editor.nbt.TagEditor
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.TAG_STRING
import com.mithrilmania.blocktopograph.nbt.util.appendQuoted

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Editor(editor: NBTEditorModel, sheetState: SheetState) {
        val textFieldState = rememberTextFieldState(value)
        LaunchedEffect(value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
        TagEditor(
            sheetState = sheetState,
            textFieldState = textFieldState,
            lineLimits = TextFieldLineLimits.Default,
            supportingText = {
                Spacer(Modifier.height(with(LocalDensity.current) {
                    LocalTextStyle.current.lineHeight.toDp()
                }))
            }
        ) {
            editor.performOperation(
                Assign(textFieldState.text.toString())
            )
            it()
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