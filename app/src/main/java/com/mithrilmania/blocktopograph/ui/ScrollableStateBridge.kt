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
sealed class ScrollableStateBridge<T : ScrollableState>(
    context: Context
) : ViewGroup(context), ScrollingView {
    private var scrollableState: T? = null
    private var coroutineScope: CoroutineScope? = null

    fun bind(state: T, scope: CoroutineScope) {
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
        scrollableState?.let { state ->
            coroutineScope?.launch {
                smoothScrollTo(state, position)
            }
        }
    }

    @Suppress("unused")
    fun scrollToPosition(position: Int) {
        scrollableState?.let { state ->
            coroutineScope?.launch {
                scrollTo(state, position)
            }
        }
    }

    protected abstract suspend fun smoothScrollTo(state: T, pos: Int)

    protected abstract suspend fun scrollTo(state: T, pos: Int)
}

class ScrollStateBridge(context: Context) : ScrollableStateBridge<ScrollState>(context) {
    override suspend fun smoothScrollTo(state: ScrollState, pos: Int) {
        state.animateScrollTo(pos)
    }

    override suspend fun scrollTo(state: ScrollState, pos: Int) {
        state.scrollTo(pos)
    }
}

class LazyListStateBridge(context: Context) : ScrollableStateBridge<LazyListState>(context) {
    override suspend fun smoothScrollTo(state: LazyListState, pos: Int) {
        state.animateScrollToItem(pos)
    }

    override suspend fun scrollTo(state: LazyListState, pos: Int) {
        state.scrollToItem(pos)
    }
}

class LazyGridStateBridge(context: Context) : ScrollableStateBridge<LazyGridState>(context) {
    override suspend fun smoothScrollTo(state: LazyGridState, pos: Int) {
        state.animateScrollToItem(pos)
    }

    override suspend fun scrollTo(state: LazyGridState, pos: Int) {
        state.scrollToItem(pos)
    }
}
