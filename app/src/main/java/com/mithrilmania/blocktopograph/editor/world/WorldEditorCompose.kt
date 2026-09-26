package com.mithrilmania.blocktopograph.editor.world

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.MapFragment
import com.mithrilmania.blocktopograph.map.analyzeChunksImpl
import com.mithrilmania.blocktopograph.map.picer.PicerState
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.runSuppressing
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.WorldModel
import com.mithrilmania.blocktopograph.world.resolveLocalPlayerPos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FloatingActionButtonMenuScope.WorldEditorMenuItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    a11yAction: CustomAccessibilityAction? = null,
    onClick: () -> Unit
) {
    FloatingActionButtonMenuItem(
        modifier =
            modifier
                .semantics {
                    isTraversalGroup = true
                    // Add a custom a11y action to allow closing the menu when focusing
                    // the last menu item, since the close button comes before the first
                    // menu item in the traversal order.
                    a11yAction?.let {
                        customActions = listOf(it)
                    }
                },
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(label) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldEditorMenu(
    viewer: WorldViewerModel,
    handle: WorldModel,
    modifier: Modifier,
    fragment: () -> MapFragment,
    onShow: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(menuExpanded) { menuExpanded = false }

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = menuExpanded,
        button = {
            TooltipBox(
                "Toggle menu",
                if (menuExpanded) {
                    TooltipAnchorPosition.Start
                } else {
                    TooltipAnchorPosition.Above
                }
            ) {
                ToggleFloatingActionButton(
                    modifier =
                        Modifier
                            .semantics {
                                traversalIndex = -1f
                                stateDescription =
                                    if (menuExpanded) "Expanded" else "Collapsed"
                                contentDescription = "Toggle menu"
                            }
                            .focusRequester(focusRequester),
                    checked = menuExpanded,
                    onCheckedChange = {
                        menuExpanded = !menuExpanded
                        if (menuExpanded) {
                            onShow()
                        }
                    },
                ) {
                    val imageVector by remember {
                        derivedStateOf {
                            if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                        }
                    }
                    Icon(
                        painter = rememberVectorPainter(imageVector),
                        contentDescription = null,
                        modifier = Modifier.animateIcon({ checkedProgress }),
                    )
                }
            }
        }
    ) {
        val context = LocalContext.current
        WorldEditorMenuItem(
            Icons.Filled.Home,
            stringResource(R.string.go_to_spawn),
            Modifier.onKeyEvent {
                // Navigating back from the first item should go back to the
                // FAB menu button.
                if (it.type == KeyEventType.KeyDown
                    && ((it.isShiftPressed && it.key == Key.Tab)
                            || it.key == Key.DirectionUp
                            || it.key == Key.NumPadDirectionUp)
                ) {
                    focusRequester.requestFocus()
                    return@onKeyEvent true
                }
                return@onKeyEvent false
            }
        ) {
            coroutineScope.launch(Dispatchers.IO) {
                val tags = handle.world.config.getCached(context)
                val spawnX = tags.getTyped<NumericTag>("SpawnX")
                val spawnY = tags.getTyped<NumericTag>("SpawnY")
                val spawnZ = tags.getTyped<NumericTag>("SpawnZ")
                if (spawnX === null || spawnY === null || spawnZ === null) {
                    withContext(Dispatchers.Main) {
                        context.toast(R.string.failed_find_spawn)
                    }
                    return@launch
                }
                val x = spawnX.toInt()
                var y = spawnY.toInt()
                val z = spawnZ.toInt()
                if (y >= 256) runSuppressing {
                    val chunk = handle.world.storage
                        ?.getChunk(x shr 4, z shr 4, VanillaDimension.OVERWORLD)
                    if (chunk !== null && !chunk.isError) {
                        y = chunk.getHeightMapValue(x and 0xF, z and 0xF) + 1
                    }
                }
                withContext(Dispatchers.Main) {
                    fragment().moveCameraToSpawn(VanillaDimension.OVERWORLD, x, y, z)
                }
            }
            menuExpanded = false
        }
        WorldEditorMenuItem(
            Icons.Filled.MyLocation,
            stringResource(R.string.go_to_player)
        ) {
            coroutineScope.launch(Dispatchers.IO) {
                val pos = try {
                    handle.world.resolveLocalPlayerPos(context)
                } catch (e: Exception) {
                    Log.d(APP_TAG, "Failed to locate local player", e)
                    null
                }
                if (pos === null) {
                    withContext(Dispatchers.Main) {
                        context.toast(R.string.failed_find_player)
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    fragment().moveCameraToPlayer(pos)
                }
            }
            menuExpanded = false
        }
        WorldEditorMenuItem(
            Icons.Filled.Search,
            stringResource(R.string.gps_advanced_locator)
        ) {
            fragment().openAdvancedLocator()
            menuExpanded = false
        }
        WorldEditorMenuItem(
            Icons.Filled.Satellite,
            stringResource(R.string.gps_picer),
            a11yAction = CustomAccessibilityAction("Close menu") {
                menuExpanded = false
                true
            }
        ) {
            viewer.picerState = PicerState.Analyzing(
                viewer.viewModelScope.launch(Dispatchers.IO) {
                    val area = try {
                        handle.world.analyzeChunksImpl(viewer.dimension)
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (_: IllegalStateException) {
                        viewer.picerState =
                            PicerState.Failed(R.string.picer_failed_corrupt)
                        return@launch
                    } catch (_: UnsupportedOperationException) {
                        viewer.picerState =
                            PicerState.Failed(R.string.picer_failed_old)
                        return@launch
                    } catch (_: Exception) {
                        viewer.picerState = null
                        withContext(Dispatchers.Main) {
                            viewer.application.toast(R.string.picer_failed_nodata)
                        }
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        viewer.commitAnalyzedState(
                            IntRect(area.left, area.top, area.right, area.bottom),
                            PicerState.WorldOutOfSize
                        )
                    }
                }
            )
            menuExpanded = false
        }
    }
}