package com.mithrilmania.blocktopograph.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.mithrilmania.blocktopograph.R

val fadeInAndExpandVertically = fadeIn() + expandVertically()
val fadeOutAndShrinkVertically = fadeOut() + shrinkVertically()

@Composable
fun ExpanderIndicator(expanded: Boolean) {
    Icon(
        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
        contentDescription = stringResource(if (expanded) R.string.expander_collapse else R.string.expander_expand),
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
        contentDescription = stringResource(if (expanded) R.string.expander_collapse else R.string.expander_expand),
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