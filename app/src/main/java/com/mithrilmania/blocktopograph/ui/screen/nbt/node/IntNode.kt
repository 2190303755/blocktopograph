package com.mithrilmania.blocktopograph.ui.screen.nbt.node

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.TAG_INT
import com.mithrilmania.blocktopograph.ui.screen.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.ui.screen.nbt.Operation
import com.mithrilmania.blocktopograph.ui.screen.nbt.TagEditor
import com.mithrilmania.blocktopograph.util.isNumber
import com.mithrilmania.blocktopograph.util.todoI18n

class IntNode(
    parent: RootLike,
    key: Any,
    value: Int
) : ValueNode(parent, key) {
    var value: Int by mutableIntStateOf(value)
    override val type: Byte get() = TAG_INT
    override fun toBinaryTag(): IntTag = IntTag(this.value)

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_int)

    @Composable
    override fun summary() = value.toString()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Editor(editor: NBTEditorModel, sheetState: SheetState) {
        val textFieldState = rememberTextFieldState(value.toString())
        var isError by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value.toString())
        }
        LaunchedEffect(Unit) {
            snapshotFlow { textFieldState.text }.collect {
                isError = it.trim().toString().toIntOrNull() === null
            }
        }
        TagEditor(
            sheetState = sheetState,
            textFieldState = textFieldState,
            isError = isError,
            supportingText = {
                Text(
                    if (isError) "Invalid int".todoI18n() else "",
                    Modifier.clearAndSetSemantics {})
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberSigned,
                imeAction = ImeAction.Done
            ),
            inputTransformation = {
                if (!this.asCharSequence().isNumber()) {
                    this.revertAllChanges()
                }
            }
        ) {
            val assign = textFieldState.text.trim().toString().toIntOrNull()
            if (assign === null) {
                isError = true
            } else {
                editor.performOperation(Assign(assign))
                it()
            }
        }
    }

    inner class Assign(val neo: Int) : Operation {
        val old: Int = this@IntNode.value
        override fun redo(editor: NBTEditorModel) {
            this@IntNode.value = this.neo
        }

        override fun undo(editor: NBTEditorModel) {
            this@IntNode.value = this.old
        }
    }
}