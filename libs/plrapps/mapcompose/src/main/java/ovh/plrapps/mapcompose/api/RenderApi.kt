@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.annotation.MainThread
import androidx.compose.ui.graphics.ColorFilter
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.core.ColorFilterProvider
import ovh.plrapps.mapcompose.ui.state.MapModel

/**
 * Reloads all tiles.
 */
fun MapModel.reloadTiles() {
    viewModelScope.launch {
        tileCanvasState.forgetTiles()
        renderVisibleTilesThrottled()
    }
}

/**
 * Controls the fade-in effect of tiles. Provided speed should be in the range [0.01f, 1.0f].
 * Values below 0.04f aren't recommended (can cause blinks), the default is 0.07f.
 * A [speed] of 1f effectively disables the fade-in effect.
 */
@MainThread
fun MapModel.setFadeInSpeed(speed: Float) {
    tileCanvasState.alphaTick = speed
}

/**
 * Disables the fade-in effect of tiles.
 */
@MainThread
fun MapModel.disableFadeIn() {
    tileCanvasState.alphaTick = 1f
}

/**
 * Applies a [ColorFilter] for each tile. A different [ColorFilter] can be applied depending on the
 * coordinate of tiles.
 * This change triggers a re-composition (effects are immediately visible).
 */
fun MapModel.setColorFilterProvider(provider: ColorFilterProvider) {
    tileCanvasState.colorFilterProvider = provider
}

/**
 * Virtually increase the size of the screen by a padding in pixel amount.
 * With the appropriate value, this can be used to produce a seamless tile loading effect.
 *
 * @param padding in pixels
 */
@MainThread
fun MapModel.setPreloadingPadding(padding: Int) {
    preloadingPadding = padding.coerceAtLeast(0)
    renderVisibleTilesThrottled()
}

/* `setMagnifyingFactor` is gone: the factor is immutable resolver configuration, injected via
 * the MapModel constructor. */
