package com.mithrilmania.blocktopograph.editor.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.MapFragment
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme

class LongPressPos(
    @JvmField val worldX: Double,
    @JvmField val worldZ: Double,
    @JvmField val chunkX: Int,
    @JvmField val chunkZ: Int
)

@Composable
fun LongPressDialog(
    viewer: WorldViewerModel,
    fragment: MapFragment
) {
    viewer.longPressPos?.let { pos ->
        BlocktopographCompatTheme(darkTheme = true) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                val onDismissRequest = { viewer.longPressPos = null }
                BasicAlertDialog(
                    onDismissRequest = onDismissRequest,
                    properties = DialogProperties(scrimAlpha = 0.3F)
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color(0, 0, 0, 0x40))
                    ) {
                        Text(
                            stringResource(
                                R.string.postion_2D_floats_with_chunkpos,
                                pos.worldX,
                                pos.worldZ,
                                pos.chunkX,
                                pos.chunkZ
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        val itemPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        remember {
                            arrayOf(
                                R.string.teleport_local_player to {
                                    fragment.onChooseTeleportPlayer(
                                        pos.worldX.toFloat(),
                                        pos.worldZ.toFloat(),
                                        viewer.dimension
                                    )
                                    viewer.longPressPos = null
                                },
                                R.string.create_custom_marker to {
                                    fragment.onChooseAddMarker(
                                        pos.worldX.toInt(),
                                        pos.worldZ.toInt(),
                                        viewer.dimension
                                    )
                                    viewer.longPressPos = null
                                },
                                R.string.open_chunk_entity_nbt to {
                                    fragment.onChooseEditEntitiesOrTileEntities(
                                        viewer.dimension,
                                        pos.chunkX,
                                        pos.chunkZ,
                                        true
                                    )
                                    viewer.longPressPos = null
                                },
                                R.string.open_chunk_tile_entity_nbt to {
                                    fragment.onChooseEditEntitiesOrTileEntities(
                                        viewer.dimension,
                                        pos.chunkX,
                                        pos.chunkZ,
                                        false
                                    )
                                    viewer.longPressPos = null
                                }
                            )
                        }.forEach {
                            ListItem(
                                onClick = it.second,
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                contentPadding = itemPadding
                            ) {
                                Text(stringResource(it.first))
                            }
                        }
                        ListItem(
                            onClick = {
                                fragment.beginOrEndSelection(
                                    pos.worldX.toInt(),
                                    pos.worldZ.toInt()
                                )
                                viewer.longPressPos = null
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            contentPadding = itemPadding
                        ) {
                            Text(
                                stringResource(
                                    if (viewer.paneType == PaneType.SELECTOR) R.string.func_cancel_selection
                                    else R.string.func_begin_selection
                                )
                            )
                        }
                        TextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .align(Alignment.End)
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}