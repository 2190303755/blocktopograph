package com.mithrilmania.blocktopograph.editor.dialog

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.EndTag
import com.mithrilmania.blocktopograph.nbt.KINDS_OF_SELECTABLE_TAGS
import com.mithrilmania.blocktopograph.nbt.TAG_END
import com.mithrilmania.blocktopograph.nbt.toTagType
import com.mithrilmania.blocktopograph.nbt.util.parseSNBT
import com.mithrilmania.blocktopograph.ui.component.PastableDialog
import com.mithrilmania.blocktopograph.util.collectText

fun MutableIntState.transform(pasted: BinaryTag, source: NBTNode? = null): BinaryTag =
    this.intValue.toByte().toTagType().transform(
        if (source === null || pasted !== EndTag) pasted else source.toBinaryTag()
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagTypePicker(
    type: MutableIntState,
    exclude: Int = TAG_END.toInt()
) {
    val localized = stringArrayResource(R.array.tag_types)
    var expanded by remember { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(localized.getOrNull(type.intValue - 1) ?: "")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            state = textFieldState,
            readOnly = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(R.string.option_tag_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MenuDefaults.groupStandardContainerColor,
            shape = MenuDefaults.standaloneGroupShape,
        ) {
            val skip = exclude - 1
            repeat(KINDS_OF_SELECTABLE_TAGS) {
                if (skip == it) return@repeat
                val name = localized.getOrNull(it) ?: ""
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(it, KINDS_OF_SELECTABLE_TAGS),
                    text = {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    },
                    selected = it + 1 == type.intValue,
                    onClick = {
                        textFieldState.setTextAndPlaceCursorAtEnd(name)
                        type.intValue = it + 1
                        expanded = false
                    },
                    selectedLeadingIcon = {
                        Icon(
                            Icons.Filled.Check,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun TagNameInputField(
    name: TextFieldState,
    duplicate: MutableState<Boolean>,
    validator: (CharSequence) -> Boolean
) {
    LaunchedEffect(Unit) {
        snapshotFlow { name.text }.collect {
            duplicate.value = validator(it)
        }
    }
    OutlinedTextField(
        state = name,
        isError = duplicate.value,
        label = {
            Text(stringResource(R.string.option_tag_name))
        },
        supportingText = {
            Text(if (duplicate.value) stringResource(R.string.error_duplicate_key) else "")
        },
        onKeyboardAction = {
            validator(name.text)
        }
    )
}

@Composable
fun TagPickerDialogLayout(
    title: String,
    isValid: Boolean,
    onPaste: (Pair<String, BinaryTag>) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    PastableDialog(
        title = title,
        isValid = isValid,
        onPaste = {
            it?.collectText()?.parseSNBT()?.let(onPaste)
        },
        onCancel = onCancel,
        onConfirm = onConfirm,
        properties = DialogProperties(dismissOnClickOutside = false),
        content = content,
    )
}

@Composable
fun TagPickerDialog(
    title: String,
    initial: Int,
    onCancel: () -> Unit,
    source: NBTNode? = null,
    exclude: Boolean = false,
    onConfirm: (BinaryTag) -> Unit
) {
    var pasted by remember { mutableStateOf<BinaryTag>(EndTag) }
    val type = rememberSaveable { mutableIntStateOf(initial) }
    TagPickerDialogLayout(
        title = title,
        onCancel = onCancel,
        isValid = type.intValue in 1..KINDS_OF_SELECTABLE_TAGS,
        onPaste = {
            pasted = it.second
            val typeId = it.second.type.typeId.toInt()
            if (!exclude || typeId != initial) {
                type.intValue = typeId
            }
        },
        onConfirm = {
            onConfirm(type.transform(pasted, source))
        }
    ) {
        TagTypePicker(type, if (exclude) initial else TAG_END.toInt())
    }
}

@Composable
fun NBTPickerDialog(
    title: String,
    validator: (CharSequence) -> Boolean,
    onCancel: () -> Unit,
    onConfirm: (String, BinaryTag) -> Unit
) {
    val duplicate = rememberSaveable { mutableStateOf(false) }
    var pasted by remember { mutableStateOf<BinaryTag>(EndTag) }
    val name = rememberTextFieldState()
    val type = rememberSaveable { mutableIntStateOf(TAG_END.toInt()) }
    TagPickerDialogLayout(
        title = title,
        onCancel = onCancel,
        isValid = !duplicate.value && type.intValue in 1..KINDS_OF_SELECTABLE_TAGS,
        onPaste = {
            name.setTextAndPlaceCursorAtEnd(it.first)
            pasted = it.second
            type.intValue = it.second.type.typeId.toInt()
        },
        onConfirm = {
            onConfirm(
                name.text.toString(),
                type.transform(pasted)
            )
        }
    ) {
        Column {
            TagTypePicker(type)
            TagNameInputField(name, duplicate, validator)
        }
    }
}