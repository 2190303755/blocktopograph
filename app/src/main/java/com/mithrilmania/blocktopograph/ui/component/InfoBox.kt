package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Descriptor(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    enableMarquee: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = if (enableMarquee) Modifier.basicMarquee() else Modifier
        )
    }
}

@Composable
fun Indicator(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = modifier.size(20.dp)
    )
}

@Composable
fun InfoBar(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enableMarquee: Boolean = false,
    spacing: Dp = 8.dp,
    icon: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.invoke(this)
        if (description === null) {
            Text(
                text = title,
                modifier = Modifier.weight(1.0F),
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Descriptor(title, description, Modifier.weight(1.0F), enableMarquee)
        }
        footer?.invoke(this)
    }
}

@Composable
fun InfoBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
        content = content
    )
}

fun Modifier.applyInfoBarPadding(
    horizontal: Dp = 16.dp,
    vertical: Dp = 12.dp,
) = this.padding(horizontal, vertical)

fun Modifier.applyInfoBoxPadding(
    padding: Dp = 16.dp
): Modifier = this.padding(padding)