package com.mithrilmania.blocktopograph.ui.screen.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Browser
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ShizukuStatus
import com.mithrilmania.blocktopograph.rememberShizukuStatus
import com.mithrilmania.blocktopograph.settings.FlagStore
import com.mithrilmania.blocktopograph.settings.asFlagState
import com.mithrilmania.blocktopograph.settings.setFlag
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.excludeTop
import com.mithrilmania.blocktopograph.ui.component.onlyTop
import com.mithrilmania.blocktopograph.ui.rememberBridgedLazyListState
import com.mithrilmania.blocktopograph.util.toast
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

@Composable
fun SettingsScreen(onNavigate: (AppNavKey) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = ::AppBarNavigationButton
            )
        }
    ) { padding ->
        Box(contentAlignment = Alignment.TopCenter) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
            val shizukuStatus = rememberShizukuStatus {
                coroutineScope.launch {
                    FlagStore.IS_SHIZUKU_ALLOWED.setFlag(context, true)
                }
            }
            val isShizukuAllowed by FlagStore.IS_SHIZUKU_ALLOWED.asFlagState(context, false)
            val isShizukuActivated by remember {
                derivedStateOf {
                    isShizukuAllowed && shizukuStatus.value == ShizukuStatus.AVAILABLE
                }
            }
            val keys = remember {
                arrayOf(
                    AppNavKey.WorkspaceManager,
                    AppNavKey.SAFManager,
                    AppNavKey.ScanningManager
                )
            }
            LazyColumn(
                modifier = Modifier
                    .padding(padding.onlyTop())
                    .widthIn(max = 640.dp),
                state = rememberBridgedLazyListState(),
                contentPadding = padding.excludeTop() + PaddingValues(
                    start = 12.dp,
                    top = 10.dp,
                    bottom = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                item {
                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    val onCheckedChange: (Boolean) -> Unit = {
                        when (shizukuStatus.value) {
                            ShizukuStatus.UNAUTHORIZED -> {
                                if (Shizuku.shouldShowRequestPermissionRationale()) {
                                    // TODO: show instruction
                                } else {
                                    Shizuku.requestPermission(1)
                                }
                            }

                            ShizukuStatus.AVAILABLE -> coroutineScope.launch {
                                FlagStore.IS_SHIZUKU_ALLOWED.setFlag(context, it)
                            }

                            else -> {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://shizuku.rikka.app/".toUri()
                                ).putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                                try {
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    context.toast(R.string.general_failed) // TODO toast: Failed to launch browser
                                }
                            }
                        }
                    }
                    val isShizukuValid by remember {
                        derivedStateOf {
                            val status = shizukuStatus.value
                            status == ShizukuStatus.AVAILABLE || status == ShizukuStatus.UNAUTHORIZED
                        }
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    SegmentedListItem(
                        checked = isShizukuActivated,
                        onCheckedChange = onCheckedChange,
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                        colors = colors,
                        modifier = Modifier
                            .animateItem()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        interactionSource = interactionSource,
                        leadingContent = { Icon(Icons.Filled.Adb, null) },
                        trailingContent = {
                            if (isShizukuValid) {
                                Switch(
                                    checked = isShizukuActivated,
                                    onCheckedChange = onCheckedChange,
                                    interactionSource = interactionSource
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                            }
                        },
                        supportingContent = {
                            Text(
                                stringResource(
                                    when (shizukuStatus.value) {
                                        ShizukuStatus.UNKNOWN -> R.string.settings_shizuku_unknown
                                        ShizukuStatus.UNSUPPORTED -> R.string.settings_shizuku_unsupported
                                        ShizukuStatus.UNAUTHORIZED -> R.string.settings_shizuku_unauthorized
                                        ShizukuStatus.AVAILABLE -> if (isShizukuAllowed) {
                                            R.string.settings_shizuku_available_allowed
                                        } else {
                                            R.string.settings_shizuku_available_disallowed
                                        }
                                    }
                                )
                            )
                        },
                        content = {
                            Text(
                                stringResource(
                                    when (shizukuStatus.value) {
                                        ShizukuStatus.UNKNOWN -> R.string.settings_shizuku_status_unknown
                                        ShizukuStatus.UNSUPPORTED -> R.string.settings_shizuku_status_unsupported
                                        ShizukuStatus.UNAUTHORIZED -> R.string.settings_shizuku_status_unauthorized
                                        ShizukuStatus.AVAILABLE -> R.string.settings_shizuku_status_available
                                    }
                                )
                            )
                        }
                    )
                }
                itemsIndexed(keys, key = { _, key -> key.title }) { index, key ->
                    SegmentedListItem(
                        onClick = { onNavigate(key) },
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = keys.size),
                        modifier = Modifier.animateItem(),
                        colors = colors,
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = { Icon(key.icon, null) },
                        supportingContent = { Text(stringResource(key.description)) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowRight, null) },
                        content = { Text(stringResource(key.title)) },
                    )
                }
            }
        }
    }
}
