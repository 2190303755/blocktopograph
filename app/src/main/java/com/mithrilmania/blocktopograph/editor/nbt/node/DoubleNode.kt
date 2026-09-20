package com.mithrilmania.blocktopograph.editor.nbt.node

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.editor.nbt.Operation
import com.mithrilmania.blocktopograph.editor.nbt.TagEditor
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.TAG_DOUBLE
import com.mithrilmania.blocktopograph.util.isDecimal

class DoubleNode(
    parent: RootLike,
    key: Any,
    value: Double
) : ValueNode(parent, key) {
    var value: Double by mutableDoubleStateOf(value)
    override val type: Byte get() = TAG_DOUBLE
    override fun toBinaryTag(): DoubleTag = DoubleTag(this.value)

    @Composable
    override fun icon() = painterResource(R.drawable.ic_tag_double)

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
                isError = it.toString().toDoubleOrNull() === null
            }
        }
        TagEditor(
            sheetState = sheetState,
            textFieldState = textFieldState,
            isError = isError,
            supportingText = {
                // TODO: i18n
                Text(if (isError) "Invalid double" else "", Modifier.clearAndSetSemantics {})
            },
            // TODO: Provide localized description of the error
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.DecimalSigned,
                imeAction = ImeAction.Done
            ),
            inputTransformation = InputTransformation.then {
                if (!this.asCharSequence().isDecimal()) {
                    this.revertAllChanges()
                }
            }
        ) {
            val assign = textFieldState.text.toString().toDoubleOrNull()
            if (assign === null) {
                isError = true
            } else {
                editor.performOperation(Assign(assign))
                it()
            }
        }
    }

    inner class Assign(val neo: Double) : Operation {
        val old: Double = this@DoubleNode.value
        override fun redo(editor: NBTEditorModel) {
            this@DoubleNode.value = this.neo
        }

        override fun undo(editor: NBTEditorModel) {
            this@DoubleNode.value = this.old
        }
    }
}