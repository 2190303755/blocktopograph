package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.mithrilmania.blocktopograph.R

val fadeInAndExpandVertically = fadeIn() + expandVertically()
val fadeOutAndShrinkVertically = fadeOut() + shrinkVertically()

@Composable
fun expanderDescription(expanded: Boolean): String = stringResource(
    if (expanded) R.string.expander_collapse else R.string.expander_expand
)

@Composable
fun ExpanderIndicator(expanded: Boolean) {
    Icon(
        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
        contentDescription = expanderDescription(expanded),
        modifier = Modifier.size(indicatorSize)
    )
}

@Composable
fun AnimatedExpanderIndicator(
    expanded: Boolean,
    size: Dp = indicatorSize
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180F else 0F,
        label = "Indicator Rotation",
    )
    Icon(
        Icons.Filled.KeyboardArrowDown,
        modifier =
            Modifier
                .size(size)
                .graphicsLayer {
                    this.rotationZ = rotation
                },
        contentDescription = expanderDescription(expanded),
    )
}

@Composable
fun Expander(
    expanded: Boolean,
    header: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = fadeInAndExpandVertically,
    exitTransition: ExitTransition = fadeOutAndShrinkVertically,
    content: @Composable () -> Unit,
) {
    InfoBox(modifier) {
        header()
        AnimatedVisibility(
            visible = expanded,
            enter = enterTransition,
            exit = exitTransition
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> Expander(
    title: String,
    items: Collection<T>,
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    selectable: Boolean = false,
    leadingContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (Boolean) -> Unit = ::ExpanderIndicator,
    itemContent: @Composable ((T, ListItemShapes) -> Unit)
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier) {
        val description = expanderDescription(expanded)
        SegmentedListItem(
            onClick = { expanded = !expanded },
            modifier = Modifier.semantics { stateDescription = description },
            shapes = ListItemDefaults.segmentedShapes(
                index = 0,
                count = if (expanded) items.size + 1 else 1
            ),
            colors = colors,
            leadingContent = leadingContent,
            supportingContent = supportingContent,
            trailingContent = { trailingContent(expanded) },
            content = { Text(title) },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
        ) {
            Column(
                modifier = (if (selectable) Modifier.selectableGroup() else Modifier)
                    .padding(top = ListItemDefaults.SegmentedGap),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                val count = items.size + 1
                items.forEachIndexed { index, item ->
                    itemContent(
                        item,
                        ListItemDefaults.segmentedShapes(index = index + 1, count = count)
                    )
                }
            }
        }
    }
}