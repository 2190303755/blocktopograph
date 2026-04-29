package com.mithrilmania.blocktopograph.map.selection

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.MapTileView
import java.lang.ref.WeakReference

interface SelectionViewCompat {
    val tileView: WeakReference<MapTileView>?
    var dragger: View?
    var dragCurrentPos: Float
    var dragBeginPosCorr: Float
    var dragAccumulation: Float
    var dragDirection: Int
    val holdingMover: Runnable
    val selectionRect: Rect
    val selectionChangedListener: SelectionChangedListener?
    val minDistToScreenBound: Int
    fun getChildAt(index: Int): View?
    fun post(action: Runnable?): Boolean
    fun postDelayed(action: Runnable?, delayMillis: Long): Boolean
    fun isEnabled(): Boolean
    fun getMeasuredWidth(): Int
    fun getMeasuredHeight(): Int
    fun requestLayout()
    fun onViewAddCompat(child: View) {
        when (child.id) {
            R.id.left, R.id.right, R.id.top, R.id.bottom -> {
                child.setOnTouchListener(this::onTouch)
            }
        }
    }

    fun onTouch(view: View, motion: MotionEvent): Boolean {
        val tileView = this.tileView?.get() ?: return false
        // If already dragging another button, disallow dragging a second one.
        val which = view.id
        // Well if we return false for an ACTION_DOWN then it won't bother popping
        // Tons of confusing ACTION_MOVEs.
        if (this.dragger?.id != which) return false
        // Set current pos.
        when (which) {
            R.id.left, R.id.right -> {
                this.dragCurrentPos = view.x + motion.x
            }

            R.id.top, R.id.bottom -> {
                this.dragCurrentPos = view.y + motion.y
            }
        }
        return when (motion.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Begin visual effect on the pressed button.
                view.isPressed = true

                // Show icon.
                this.getChildAt(4)?.apply {
                    visibility = View.VISIBLE
                    when (which) {
                        R.id.left -> {
                            rotation = 270.0F
                        }

                        R.id.right -> {
                            rotation = 90.0F
                        }

                        R.id.top -> {
                            rotation = 0.0F
                        }

                        R.id.bottom -> {
                            rotation = 180.0F
                        }
                    }
                }

                // Prevents tileView being touched while dragging.
                tileView.setTouchable(false)
                // Set current dragging item.
                this.dragger = view
                when (which) {
                    R.id.left, R.id.right -> {
                        this.dragBeginPosCorr = motion.x // - view.getX();
                    }

                    R.id.top, R.id.bottom -> {
                        this.dragBeginPosCorr = motion.y // - view.getY();
                    }
                }
                dragAccumulation = 0.0F
                dragDirection = 0
                // And trigger a continuous detecting.
                this.post(this.holdingMover)
                // IMPORTANT! Forgot this once. Fuck you man, fuck you!
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // End visual effect.
                view.isPressed = false
                // Hide icon.
                this.getChildAt(4)?.visibility = View.GONE
                // Clear & Unlock.
                tileView.setTouchable(true)
                this.dragger = null
                true
            }

            MotionEvent.ACTION_MOVE -> {
                //onMove(tileView, which, motionEvent);
                true
            }

            else -> false
        }
    }

    fun preSelect(tileView: MapTileView, distOnScreen: Float, movement: Float): Int

    fun onMove() {
        // return if user no longer holding
        val dragger = this.dragger ?: return
        if (!this.isEnabled()) return
        // The tileView we serves for.
        val tileView = this.tileView?.get() ?: return

        // Get the view to retrieve its position.

        // ?

        // Retrieve view position then get the distance and screen size.

        val draggerId = dragger.id
        // Distance between view position and current holding position.
        val distOnScreen = when (draggerId) {
            R.id.left, R.id.right -> {
                // `dragged.getX() - mDragBeginPosCorr` is the current position of user's initially
                // touched point of the View.
                this.dragCurrentPos - dragger.x - this.dragBeginPosCorr
            }

            R.id.top, R.id.bottom -> {
                this.dragCurrentPos - dragger.y - this.dragBeginPosCorr
            }

            else -> return
        }
        // Amplify movement. Maybe we'd allow user to set it, or calculate based on screen
        // density and tileView's scale.
        val amp = 0.2f


        //Log.d(this, "" + distOnScreen + "," + mDragCurrentPos + "," + mDragBeginPosCorr);

        // If a previous round failed to move at least 1 oldBlock's wide,
        // it would accumulate the distance till in a future round we could move.
        val movement: Float = distOnScreen * amp + this.dragAccumulation
        var distanceInBlocks = this.preSelect(tileView, distOnScreen, movement)
        if (distanceInBlocks != 0) {
            // Alter selection.
            // Selection shall be at least 1x1.
            val mSelectionRect = this.selectionRect
            when (draggerId) {
                R.id.left -> {
                    if (mSelectionRect.left + distanceInBlocks >= mSelectionRect.right) {
                        mSelectionRect.left = mSelectionRect.right - 1
                        distanceInBlocks = 0
                    } else mSelectionRect.left += distanceInBlocks
                }

                R.id.right -> {
                    if (mSelectionRect.right + distanceInBlocks <= mSelectionRect.left) {
                        mSelectionRect.right = mSelectionRect.left + 1
                        distanceInBlocks = 0
                    } else mSelectionRect.right += distanceInBlocks
                }

                R.id.top -> {
                    if (mSelectionRect.top + distanceInBlocks >= mSelectionRect.bottom) {
                        mSelectionRect.top = mSelectionRect.bottom - 1
                        distanceInBlocks = 0
                    } else mSelectionRect.top += distanceInBlocks
                }

                R.id.bottom -> {
                    if (mSelectionRect.bottom + distanceInBlocks <= mSelectionRect.top) {
                        mSelectionRect.bottom = mSelectionRect.top + 1
                        distanceInBlocks = 0
                    } else mSelectionRect.bottom += distanceInBlocks
                }
            }

            // If no movement, return.
            // It would be caused by the "Selection must be at least 1x1" rule.
            // For instance in case it's already 200x1 we can't move vertically.
            if (distanceInBlocks == 0) return
            // Notify outsides.
            this.selectionChangedListener?.onSelectionChanged(mSelectionRect)
            // Should we move the underlying tileView as well?
            // If touched point is near the moving direction (not the dragger position)
            // then we scroll.
            val sw = this.getMeasuredWidth()
            val sh = this.getMeasuredHeight()
            val minw = this.minDistToScreenBound.coerceAtLeast(sw / 8)
            val minh = this.minDistToScreenBound.coerceAtLeast(sh / 8)
            when (draggerId) {
                R.id.left, R.id.right -> {
                    // (Moving right and near right bound) or
                    // (Moving left and near left bound)
                    if (dragDirection > 0 && sw - dragCurrentPos < minw
                        || (dragDirection < 0 && dragCurrentPos < minw)
                    ) {
                        tileView.scrollX = (tileView.scrollX + movement).toInt()
                    } else requestLayout()
                }

                R.id.top, R.id.bottom -> {
                    if (dragDirection > 0 && sh - dragCurrentPos < minh
                        || (dragDirection < 0 && dragCurrentPos < minh)
                    ) {
                        tileView.scrollY = (tileView.scrollY + movement).toInt()
                    } else requestLayout()
                }
            }
        }
        this.postDelayed(this.holdingMover, 40)
    }
}