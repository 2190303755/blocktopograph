package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.editor.nbt.Operation
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.TAG_SHORT
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.util.isNumber

class ShortNode(
    parent: RootLike,
    key: Any,
    value: Short
) : ValueNode(parent, key) {
    var value: Short by mutableStateOf(value)
    override val type: Byte get() = TAG_SHORT
    override fun toBinaryTag(): ShortTag = ShortTag(this.value)

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_short)

    @Composable
    override fun summary() = value.toString()

    @Composable
    override fun Editor(editor: NBTEditorModel) {
        val focusRequester = remember { FocusRequester() }
        val textFieldState = rememberTextFieldState(value.toString())
        var isError by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value.toString())
        }
        LaunchedEffect(Unit) {
            snapshotFlow { textFieldState.text }.collect {
                isError = it.toString().toShortOrNull() === null
            }
        }
        OutlinedTextField(
            state = textFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            shape = OutlinedTextFieldDefaults.roundedShape,
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = isError,
            supportingText = {
                // TODO: i18n
                Text(if (isError) "Invalid short" else "", Modifier.clearAndSetSemantics {})
            },
            // TODO: Provide localized description of the error
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberSigned,
                imeAction = ImeAction.Done
            ),
            inputTransformation = InputTransformation.then {
                if (!this.asCharSequence().isNumber()) {
                    this.revertAllChanges()
                }
            },
            onKeyboardAction = {
                val assign = textFieldState.text.toString().toShortOrNull()
                if (assign === null) {
                    isError = true
                } else {
                    editor.performOperation(Assign(assign))
                    it()
                }
            },
            trailingIcon = {
                IconButton(Icons.Filled.Check) {
                    val assign = textFieldState.text.toString().toShortOrNull()
                    if (assign === null) {
                        isError = true
                    } else {
                        editor.performOperation(Assign(assign))
                    }
                }
            }
        )
    }

    inner class Assign(val neo: Short) : Operation {
        val old: Short = this@ShortNode.value
        override fun redo(editor: NBTEditorModel) {
            this@ShortNode.value = this.neo
        }

        override fun undo(editor: NBTEditorModel) {
            this@ShortNode.value = this.old
        }
    }
}