package com.mithrilmania.blocktopograph.ui.component

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.Dp
import com.mithrilmania.blocktopograph.util.todoI18n

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarNavigationButton() {
    val owner = LocalOnBackPressedDispatcherOwner.current
    TooltipBox("Navigate Up".todoI18n(), TooltipAnchorPosition.Below) { tooltip ->
        IconButton(onClick = { owner?.onBackPressedDispatcher?.onBackPressed() }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tooltip)
        }
    }
}

@Composable
fun BottomSheetActionButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = ButtonDefaults.MinHeight,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(height),
        modifier = modifier.heightIn(height),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = ButtonDefaults.contentPaddingFor(
            buttonHeight = height,
            hasStartIcon = icon !== null
        )
    ) {
        icon?.let {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(height)))
        }
        Text(text, style = ButtonDefaults.textStyleFor(height))
    }
}