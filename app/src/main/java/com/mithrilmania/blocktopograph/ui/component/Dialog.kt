package com.mithrilmania.blocktopograph.ui.component

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

val DialogPadding = PaddingValues(all = 24.dp)
val IconPadding = PaddingValues(bottom = 16.dp)
val TitlePadding = PaddingValues(bottom = 16.dp)
val TextPadding = PaddingValues(bottom = 24.dp)

@Composable
fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    neutralButton: @Composable () -> Unit,
    negativeButton: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    title: @Composable (BoxScope.() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
    content: @Composable BoxScope.() -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties
    ) {
        Surface(shape = shape, color = containerColor, tonalElevation = tonalElevation) {
            Column(modifier = Modifier.padding(DialogPadding)) {
                icon?.let {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(
                            modifier = Modifier
                                .padding(IconPadding)
                                .align(Alignment.CenterHorizontally),
                            content = icon
                        )
                    }
                }
                title?.let {
                    ProvideContentColorTextStyle(
                        contentColor = titleContentColor,
                        textStyle = MaterialTheme.typography.headlineSmall,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(TitlePadding)
                                .align(
                                    if (icon == null) {
                                        Alignment.Start
                                    } else {
                                        Alignment.CenterHorizontally
                                    }
                                ),
                            content = title
                        )
                    }
                }
                ProvideContentColorTextStyle(
                    contentColor = textContentColor,
                    textStyle = MaterialTheme.typography.bodyMedium,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(TextPadding)
                            .align(Alignment.Start),
                        content = content
                    )
                }
                Box(modifier = Modifier.align(Alignment.End)) {
                    ProvideContentColorTextStyle(
                        contentColor = MaterialTheme.colorScheme.primary,
                        textStyle = MaterialTheme.typography.labelLarge
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            neutralButton()
                            Spacer(modifier = Modifier.weight(1.0F))
                            negativeButton()
                            positiveButton()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PastableDialog(
    title: String,
    onPaste: (ClipData?) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    isValid: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    content: @Composable BoxScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        neutralButton = {
            val clipboard = LocalClipboard.current
            val scope = rememberCoroutineScope()
            TextButton(text = stringResource(android.R.string.paste), onClick = {
                scope.launch { onPaste(clipboard.getClipEntry()?.clipData) }
            })
        },
        positiveButton = {
            TextButton(
                text = stringResource(android.R.string.ok),
                enabled = isValid,
                onClick = onConfirm
            )
        },
        negativeButton = {
            TextButton(text = stringResource(android.R.string.cancel), onClick = onCancel)
        },
        properties = properties,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> AnimatedBottomSheetDialog(
    targetState: T?,
    enabledValues: Set<SheetValue> = AllSheetValues,
    content: @Composable (SheetState, T) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = enabledValues
    )
    var effectiveState by remember { mutableStateOf(targetState) }
    LaunchedEffect(targetState) {
        if (targetState === null) {
            coroutineScope.launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        effectiveState = null
                    }
                }
        } else if (effectiveState === null) {
            effectiveState = targetState
        } else {
            effectiveState = targetState
            coroutineScope.launch {
                sheetState.expand()
            }
        }
    }
    effectiveState?.let {
        content(sheetState, it)
    }
}