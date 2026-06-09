package com.mithrilmania.blocktopograph.ui

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridConfigurationScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_DEFAULT_FORMAT
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_DETECT_HEADER
import com.mithrilmania.blocktopograph.EXTRA_EDITOR_SKIP_IMPORTER
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorActivity
import com.mithrilmania.blocktopograph.editor.world.v2.WorldEditorActivity
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.world.WorldDetail
import kotlinx.coroutines.launch
import java.util.Date
import com.mithrilmania.blocktopograph.editor.world.WorldEditorActivity as OldWorldEditorActivity

@Composable
fun WorldDetailEntry(
    icon: ImageVector,
    label: String,
    content: String,
    modifier: Modifier = Modifier,
    copyable: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = CardDefaults.outlinedShape,
        border = CardDefaults.outlinedCardBorder(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = (if (copyable) {
                val clipboard = LocalClipboard.current
                val coroutineScope = rememberCoroutineScope()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    val context = LocalContext.current
                    Modifier.clickable {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, content)))
                            context.toast(R.string.toast_copy_success)
                        }
                    }
                } else {
                    Modifier.clickable {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, content)))
                        }
                    }
                }
            } else {
                Modifier
            })
                .fillMaxWidth()
                .padding(
                    vertical = 6.dp,
                    horizontal = 12.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label)
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGridApi::class)
@Composable
fun WorldDetailDialog(
    detail: WorldDetail,
    state: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state
    ) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .weight(1.0F, false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val grid: GridConfigurationScope.() -> Unit = {
                gap(6.dp)
                if (constraints.maxWidth > 450.dp.toPx()) {
                    column(0.5F)
                    column(0.5F)
                } else {
                    column(1.0F)
                }
            }
            Grid(config = grid) {
                val shape = MaterialTheme.shapes.medium
                val center = Modifier.gridItem(alignment = Alignment.Center)
                val shaped = center
                    .clickable { // debug. TODO remove
                        context.startActivity(
                            detail.applyTo(
                                Intent(
                                    context,
                                    OldWorldEditorActivity::class.java
                                )
                            )
                        )
                    }
                    .size(210.dp, 120.dp)
                    .border(CardDefaults.outlinedCardBorder(), shape)
                    .clip(shape)
                val icon = detail.icon
                if (icon === null) {
                    Image(
                        painter = painterResource(R.drawable.world_icon_default),
                        contentDescription = null,
                        modifier = shaped,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = shaped,
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = detail.name,
                    modifier = center,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }
            WorldDetailEntry(
                Icons.Filled.LocationOn,
                stringResource(R.string.world_detail_location),
                detail.location.location,
                copyable = true
            )
            Grid(config = grid, modifier = Modifier.padding(bottom = 6.dp)) {
                val played = remember(detail, context) {
                    Date(detail.time).let {
                        "${
                            DateFormat.getMediumDateFormat(context).format(it)
                        } ${
                            DateFormat.getTimeFormat(context).format(it)
                        } - ${detail.version}"
                    }
                }
                WorldDetailEntry(
                    Icons.Filled.AccessTime,
                    stringResource(R.string.world_detail_last_played),
                    played
                )
                WorldDetailEntry(
                    Icons.Filled.PlayCircleOutline,
                    stringResource(R.string.world_detail_game_mode),
                    detail.mode
                )
                WorldDetailEntry(
                    Icons.Filled.DataUsage,
                    stringResource(R.string.world_detail_size),
                    detail.size ?: stringResource(R.string.calculating_size)
                )
                WorldDetailEntry(
                    Icons.Filled.TravelExplore,
                    stringResource(R.string.world_detail_seed),
                    detail.seed,
                    copyable = true
                )
            }
        }
        Row(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BottomSheetActionButton(
                text = stringResource(R.string.edit_config),
                modifier = Modifier.weight(1.0F)
            ) {
                context.startActivity(
                    detail.config.applyTo(
                        Intent(
                            context,
                            NBTEditorActivity::class.java
                        ).setAction(Intent.ACTION_VIEW)
                            .putExtra(EXTRA_EDITOR_DEFAULT_FORMAT, NBTFormat.LITTLE_ENDIAN.name)
                            .putExtra(EXTRA_EDITOR_DETECT_HEADER, true)
                            .putExtra(EXTRA_EDITOR_SKIP_IMPORTER, true)
                    )
                )
            }
            BottomSheetActionButton(
                text = stringResource(R.string.edit_world),
                modifier = Modifier.weight(1.0F)
            ) {
                context.startActivity(
                    detail.applyTo(
                        Intent(
                            context,
                            WorldEditorActivity::class.java
                        )
                    )
                )
            }
        }
    }
}