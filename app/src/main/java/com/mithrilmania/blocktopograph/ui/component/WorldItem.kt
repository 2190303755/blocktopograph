package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.world.WorldDetail
import java.text.DateFormat
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
                val placeholder = painterResource(R.drawable.world_icon_default)
                AsyncImage(
                    model = detail,
                    contentDescription = null,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterVertically)
                        .size(140.dp, 80.dp)
                        .border(CardDefaults.outlinedCardBorder(), shape)
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholder,
                    fallback = placeholder,
                    error = placeholder
                )
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                    FlowRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.SpaceBetween,
                        itemVerticalAlignment = Alignment.CenterVertically,
                        maxItemsInEachRow = 2
                    ) {
                        Text(
                            text = detail.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineSmall
                        )
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
                        Text(detail.mode)
                        val time = remember(detail) {
                            DateFormat.getDateInstance(DateFormat.SHORT)
                                .format(Date(detail.time))
                        }
                        Text(time)
                    }
                }
            }
            Text(
                text = detail.location.toString(),
                modifier = Modifier.basicMarquee(Int.MAX_VALUE),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}