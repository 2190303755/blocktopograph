package com.mithrilmania.blocktopograph.ui

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.core.view.ScrollingView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Adapter for HyperOS ScrollToTopListener
 */
class ScrollableStateBridge(context: Context) : ViewGroup(context), ScrollingView {
    private var scrollableState: ScrollableState? = null
    private var coroutineScope: CoroutineScope? = null

    fun bind(state: ScrollableState, scope: CoroutineScope) {
        this.scrollableState = state
        this.coroutineScope = scope
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}

    override fun computeHorizontalScrollRange(): Int = 0

    override fun computeHorizontalScrollOffset(): Int = 0

    override fun computeHorizontalScrollExtent(): Int = 0

    override fun computeVerticalScrollRange(): Int =
        scrollableState?.scrollIndicatorState?.contentSize ?: 0

    override fun computeVerticalScrollOffset(): Int =
        scrollableState?.scrollIndicatorState?.scrollOffset ?: 0

    override fun computeVerticalScrollExtent(): Int =
        scrollableState?.scrollIndicatorState?.viewportSize ?: 0

    override fun getGlobalVisibleRect(r: Rect?, globalOffset: Point?): Boolean =
        (parent as? View)?.getGlobalVisibleRect(r, globalOffset)
            ?: super.getGlobalVisibleRect(r, globalOffset)

    @Suppress("unused")
    fun smoothScrollToPosition(position: Int) {
        coroutineScope?.launch {
            when (val state = scrollableState) {
                is LazyListState -> state.animateScrollToItem(position)
                is LazyGridState -> state.animateScrollToItem(position)
                is ScrollState -> state.animateScrollTo(position)
            }
        }
    }

    @Suppress("unused")
    fun scrollToPosition(position: Int) {
        coroutineScope?.launch {
            when (val state = scrollableState) {
                is LazyListState -> state.scrollToItem(position)
                is LazyGridState -> state.scrollToItem(position)
                is ScrollState -> state.scrollTo(position)
            }
        }
    }
}

