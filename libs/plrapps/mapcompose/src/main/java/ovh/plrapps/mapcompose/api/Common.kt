package ovh.plrapps.mapcompose.api

import androidx.compose.ui.unit.IntOffset
import ovh.plrapps.mapcompose.ui.state.VisibleAreaPadding

/**
 * When scrolling to a given position, the viewport needs to be offset by taking into account the
 * [VisibleAreaPadding]. This is needed for apis when scrolling is involved.
 */
internal fun VisibleAreaPadding.getOffsetForScroll(): IntOffset {
    return IntOffset((left - right) / 2, (top - bottom) / 2)
}
