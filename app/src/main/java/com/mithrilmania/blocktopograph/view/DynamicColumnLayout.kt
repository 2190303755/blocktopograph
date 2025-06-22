package com.mithrilmania.blocktopograph.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.mithrilmania.blocktopograph.R
import kotlin.math.max

class DynamicColumnLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : ViewGroup(context, attrs, style) {
    private var compact = true
    private val threshold: Float
    private val spacing: Int

    init {
        val resources = context.resources

        @SuppressLint("UseKtx")
        val array = context.obtainStyledAttributes(attrs, R.styleable.DynamicColumnLayout)
        this.threshold = array.getDimension(
            R.styleable.DynamicColumnLayout_threshold,
            resources.getDimension(R.dimen.bottom_sheet_threshold)
        )
        this.spacing = array.getDimensionPixelSize(
            R.styleable.DynamicColumnLayout_spacing,
            resources.getDimensionPixelSize(R.dimen.small_content_padding)
        )
        array.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val verticalPadding = this.paddingTop + this.paddingBottom
        val horizontalPadding = this.paddingLeft + this.paddingRight
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - horizontalPadding
        val count = this.childCount
        var params: LayoutParams
        var totalHeight = verticalPadding
        var childState = 0
        if (maxWidth < this.threshold) {
            this.compact = true
            for (index in 0..<count) {
                val child = this.getChildAt(index)
                params = child.layoutParams
                child.measure(
                    when (params.width) {
                        MATCH_PARENT -> MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY)
                        WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
                        else -> MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY)
                    },
                    getChildMeasureSpec(heightMeasureSpec, totalHeight, params.height)
                )
                child.measuredHeight.let {
                    totalHeight += if (it > 0) it + this.spacing else this.spacing
                }
                childState = childState or child.measuredState
            }
        } else {
            this.compact = false
            val width = (maxWidth - this.spacing) / 2
            var index = 0
            while (index < count) {
                val first = this.getChildAt(index++)
                params = first.layoutParams
                first.measure(
                    when (params.width) {
                        MATCH_PARENT -> MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
                        WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
                        else -> MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY)
                    },
                    getChildMeasureSpec(heightMeasureSpec, totalHeight, params.height)
                )
                if (index < count) {
                    val second = this.getChildAt(index++)
                    params = second.layoutParams
                    second.measure(
                        when (params.width) {
                            MATCH_PARENT -> MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
                            WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
                            else -> MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY)
                        },
                        getChildMeasureSpec(heightMeasureSpec, totalHeight, params.height)
                    )
                    max(first.measuredHeight, second.measuredHeight).let {
                        totalHeight += if (it > 0) it + this.spacing else this.spacing
                    }
                    childState = childState or first.measuredState or second.measuredState
                } else {
                    first.measuredHeight.let {
                        totalHeight += if (it > 0) it + this.spacing else this.spacing
                    }
                    childState = childState or first.measuredState
                }
            }
        }
        setMeasuredDimension(
            resolveSizeAndState(
                (this.measuredWidth + horizontalPadding).coerceAtLeast(
                    this.suggestedMinimumWidth
                ), widthMeasureSpec, childState
            ),
            resolveSizeAndState(
                totalHeight.coerceAtLeast(this.suggestedMinimumHeight),
                heightMeasureSpec,
                0
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val count = this.childCount
        var childTop = this.paddingTop
        val childLeft = this.paddingLeft
        if (this.compact) {
            val center = (childLeft + this.measuredWidth) / 2
            for (index in 0..<count) {
                val child = this.getChildAt(index)
                val childWidth = child.measuredWidth
                val childLeft = center - childWidth / 2
                val childBottom = childTop + child.measuredHeight
                child.layout(
                    childLeft,
                    childTop,
                    childLeft + childWidth,
                    childBottom
                )
                childTop = childBottom + this.spacing
            }
        } else {
            val width = this.measuredWidth
            val firstCenter = (3 * childLeft + width - this.spacing) / 4
            val secondCenter = (3 * childLeft + 3 * width + this.spacing) / 4
            var index = 0
            while (index < count) {
                val first = this.getChildAt(index++)
                if (index < count) {
                    val second = this.getChildAt(index++)
                    val childBottom = childTop + max(first.measuredHeight, second.measuredHeight)
                    var childWidth = first.measuredWidth
                    var childLeft = firstCenter - childWidth / 2
                    first.layout(childLeft, childTop, childLeft + childWidth, childBottom)
                    childWidth = second.measuredWidth
                    childLeft = secondCenter - childWidth / 2
                    second.layout(childLeft, childTop, childLeft + childWidth, childBottom)
                    childTop = childBottom + this.spacing
                } else {
                    val center = (childLeft + measuredWidth) / 2
                    val childWidth = first.measuredWidth
                    val finalLeft = center - childWidth / 2
                    val childBottom = childTop + first.measuredHeight
                    first.layout(finalLeft, childTop, finalLeft + childWidth, childBottom)
                    break
                }
            }
        }
    }
}