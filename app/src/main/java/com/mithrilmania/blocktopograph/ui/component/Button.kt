package com.mithrilmania.blocktopograph.ui.component

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun TextButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(text = text)
    }
}

@Composable
fun IconButton(
    icon: ImageVector,
    tooltip: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(imageVector = icon, contentDescription = tooltip)
    }
}

@Composable
fun AppBarNavigationButton() {
    val owner = LocalOnBackPressedDispatcherOwner.current
    TooltipBox("Navigate Up", TooltipAnchorPosition.Below) { tooltip ->
        IconButton(onClick = { owner?.onBackPressedDispatcher?.onBackPressed() }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tooltip)
        }
    }
}

@Composable
fun BottomSheetActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) { Text(text) }
}

@Composable
fun BottomSheetActionButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = ButtonDefaults.contentPaddingFor(
            buttonHeight = ButtonDefaults.MinHeight,
            hasStartIcon = true
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MinHeight)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
        Text(text)
    }
}