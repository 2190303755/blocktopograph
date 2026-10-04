package com.mithrilmania.blocktopograph.map.selection

import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntRect

abstract class Selection {
    var left by mutableIntStateOf(0)
    var top by mutableIntStateOf(0)
    var right by mutableIntStateOf(0)
    var bottom by mutableIntStateOf(0)

    fun set(rect: Rect) {
        this.left = rect.left
        this.top = rect.top
        this.right = rect.right
        this.bottom = rect.bottom
    }

    fun set(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    fun toRect(): Rect = Rect(
        this.left,
        this.top,
        this.right,
        this.bottom
    )

    fun toIntRect(): IntRect = IntRect(
        this.left,
        this.top,
        this.right,
        this.bottom
    )

    val isChunkAligned: Boolean
        get() = (this.left and 0xF) == 0
                && (this.right and 0xF) == 0
                && (this.top and 0xF) == 0
                && (this.bottom and 0xF) == 0

    abstract var isSelecting: Boolean
}
