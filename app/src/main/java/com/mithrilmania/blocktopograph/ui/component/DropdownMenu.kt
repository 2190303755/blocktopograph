package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AppBarScope
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

fun AppBarScope.clickableItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    this.clickableItem(onClick = onClick, icon = {
        Icon(imageVector = icon, contentDescription = label)
    }, label = label, enabled = enabled)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AppBarScope.cascadingMenu(
    icon: ImageVector,
    label: String,
    menu: @Composable ColumnScope.(MutableState<Boolean>) -> Unit
) {
    this.customItem(
        appbarContent = {
            Box {
                val expanded = rememberSaveable { mutableStateOf(false) }
                TooltipBox(label) {
                    IconButton(icon, label) {
                        expanded.value = true
                    }
                }
                DropdownMenu(expanded = expanded.value, onDismissRequest = {
                    expanded.value = false
                }) {
                    menu(expanded)
                }
            }
        },
        menuContent = {
            val interactionSource = remember { MutableInteractionSource() }
            val hovered by interactionSource.collectIsHoveredAsState()
            val expanded = rememberSaveable { mutableStateOf(false) }
            DropdownMenuItem(
                interactionSource = interactionSource,
                text = { Text(label) },
                onClick = { expanded.value = !expanded.value },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowRight,
                        modifier = Modifier.size(MenuDefaults.TrailingIconSize),
                        contentDescription = null,
                    )
                }
            )
            DropdownMenuPopup(
                popupPositionProvider =
                    MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                        MenuAnchorPosition.End
                    ),
                expanded = hovered || expanded.value,
                onDismissRequest = { expanded.value = false },
                properties = PopupProperties(focusable = false),
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(0, 1)
                ) {
                    menu(expanded)
                }
            }
        }
    )
}

@Composable
fun DropdownMenuItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        enabled = enabled,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(MenuDefaults.LeadingIconSize)
            )
        }
    )
}

@Composable
fun DropdownMenuItem(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        enabled = enabled,
        onClick = onClick
    )
}

@Stable
val ItemContentPadding: PaddingValues =
    PaddingValues(horizontal = 12.dp, vertical = 0.dp) // 16 - 4

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> DropdownMenuChip(
    options: Collection<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    namer: @Composable (T) -> String,
) {
    val (expanded, onExpandedChange) = remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        val minSize = LocalMinimumInteractiveComponentSize.current
        AssistChip(
            onClick = {},
            label = {
                Text(text = namer(selected))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = modifier
                .defaultMinSize(minHeight = minSize, minWidth = minSize)
                .menuAnchor(
                    ExposedDropdownMenuAnchorType.PrimaryNotEditable
                )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = MenuDefaults.groupStandardContainerColor,
            shape = MenuDefaults.standaloneGroupShape,
        ) {
            val size = options.size
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index, size),
                    text = {
                        Text(
                            text = namer(option),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.basicMarquee()
                        )
                    },
                    selected = option == selected,
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    },
                    selectedLeadingIcon = {
                        Icon(
                            Icons.Filled.Check,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                    },
                    contentPadding = ItemContentPadding,
                )
            }
        }
    }
}