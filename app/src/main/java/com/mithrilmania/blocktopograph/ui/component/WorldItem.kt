package com.mithrilmania.blocktopograph.ui.component

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.world.WorldDetail
import java.util.Date

// inline card to customize the column
@Composable
fun WorldItem(detail: WorldDetail, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = CardDefaults.elevatedCardColors()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardDefaults.outlinedShape,
        border = CardDefaults.outlinedCardBorder(),
        color = colors.containerColor,
        contentColor = colors.contentColor,
        shadowElevation = 1.dp
    ) {
        val spacing = Arrangement.spacedBy(6.dp)
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = spacing
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = spacing) {
                val shape = MaterialTheme.shapes.small
                val shaped = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .size(140.dp, 80.dp)
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
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = detail.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = spacing
                            ) {
                                val size = Modifier.size(16.dp)
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = size
                                )
                                Text(detail.resources.toString())
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                    contentDescription = null,
                                    modifier = size
                                )
                                Text(detail.behaviors.toString())
                            }
                            Text(detail.size ?: stringResource(R.string.calculating_size))
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val context = LocalContext.current
                            val time = remember(detail, context) {
                                DateFormat.getDateFormat(context).format(Date(detail.time))
                            }
                            Text(detail.mode)
                            Text(time)
                        }
                    }
                }
            }
            Text(
                text = detail.location.location,
                modifier = Modifier.basicMarquee(Int.MAX_VALUE),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}