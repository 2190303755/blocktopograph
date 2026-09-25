package com.mithrilmania.blocktopograph.map.selection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.MCTileProvider
import com.mithrilmania.blocktopograph.map.MapTileView
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.UiUtil
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.roundToInt

private fun runnable(action: Runnable) = action

class SelectionView : FrameLayout {
    /**
     * The view being dragged now.
     */
    private var mDragger: View? = null

    /**
     * Correction of drag begin point.
     * 
     * 
     * 
     * It's the relative distance between the initially touched point
     * and the view's left-top coordinates.
     * ------------------
     * |                |
     * |--[ This ]--X   |
     * |                |
     * |                |
     * ------------------
     * 
     */
    private var mDragBeginPosCorr = 0f

    /**
     * Drag distance accumulation.
     * 
     * 
     * 
     * If the drag distance of a given moment cannot move the selection
     * at least 1 oldBlock's wide, we accumulate the distance to have a larger
     * chance to move on the next round.
     * 
     */
    private var mDragAccumulation = 0f

    /**
     * Current touch position.
     * 
     * 
     * 
     * Produced in `onTouch` and consumed in `onMove`.
     * 
     */
    private var mDragCurrentPos = 0f

    /**
     * Current dragging direction.
     * 
     * 
     * 
     * Used for anti-jitter purpose.
     * 
     */
    private var mDragDirection = 0

    /**
     * Paint object used to draw selection onto canvas.
     */
    private val mPaint = Paint()

    /**
     * Selection range.
     */
    private val mSelectionRect = Rect()

    /**
     * The tileView we serves for.
     */
    private var mTileView: WeakReference<MapTileView?>? = null

    /**
     * Range of selection in pixel.
     */
    private val mSelectionPixelRange = RectF()

    /**
     * Visible range of selection in pixel.
     */
    private val mVisiblePixelRange = RectF()

    /**
     * Minimal distance between drag button and bound.
     * -----O---------------------------
     * |----| Like this.
     */
    private var BUTTON_TO_BOUND_MIN_DIST = 0f

    /**
     * The minimal non-auto-scroll distance from touched point to screen boundary.
     * 
     * 
     * 
     * Once it's exceed the tileView would be scrolled automatically.
     * 
     */
    private var MIN_DIST_TO_SCREEN_BOUND = 0

    private var MIN_DIST_DRAGGERS = 0

    private var HALF_MIN_DIST_DRAGGERS = 0

    /**
     * Runnable used to frequently alter selection while user dragging a adjust button.
     */
    private val mHoldingMover = runnable(this::onMove)

    /**
     * Indicates whether a selection exists.
     */
    private var mHasSelection = false

    /**
     * The selection could be changed outsides, so we need this.
     */
    private var mSelectionChangedListener: SelectionChangedListener? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    private fun init(context: Context) {
        // The viewGroup has static content.

        LayoutInflater.from(context).inflate(R.layout.view_selection_view, this, true)

        mPaint.setColor(Color.argb(0x80, 0, 0, 0))
        setWillNotDraw(false) // Otherwise `onDraw` won't be called.
        BUTTON_TO_BOUND_MIN_DIST = UiUtil.dpToPx(context, 72f)
        MIN_DIST_TO_SCREEN_BOUND = UiUtil.dpToPxInt(context, 100)
        MIN_DIST_DRAGGERS = UiUtil.dpToPxInt(context, 50)
        HALF_MIN_DIST_DRAGGERS = MIN_DIST_DRAGGERS / 2

        mDragger = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewAdded(child: View) {
        when (child.id) {
            R.id.left, R.id.right, R.id.top, R.id.bottom -> child.setOnTouchListener(this::onTouch)
        }
    }

    private fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val tileView: MapTileView = mTileView?.get() ?: return false

        // If already dragging another button, disallow dragging a second one.
        @IdRes val which = view.id
        if (mDragger != null && which != mDragger!!.id) {
            // Well if we return false for an ACTION_DOWN then it won't bother popping
            // Tons of confusing ACTION_MOVEs.
            return false
        }

        // Set current pos.
        when (which) {
            // Motion event's get x is relative to view's x. Sum 'em up.
            R.id.left, R.id.right -> mDragCurrentPos = view.x + motionEvent.x

            R.id.top, R.id.bottom -> mDragCurrentPos = view.y + motionEvent.y
        }

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Begin visual effect on the pressed button.
                view.setPressed(true)

                // Show icon.
                val actionIcon = getChildAt(4)
                actionIcon.visibility = VISIBLE

                when (which) {
                    R.id.left -> actionIcon.rotation = 270.0f
                    R.id.right -> actionIcon.rotation = 90.0f
                    R.id.top -> actionIcon.rotation = 0.0f
                    R.id.bottom -> actionIcon.rotation = 180.0f
                }

                // Prevents tileView being touched while dragging.
                tileView.setTouchable(false)

                // Set current dragging item.
                mDragger = view
                when (which) {
                    R.id.left, R.id.right -> mDragBeginPosCorr =
                        motionEvent.x // - view.getX();
                    R.id.top, R.id.bottom -> mDragBeginPosCorr =
                        motionEvent.y // - view.getY();
                }
                mDragAccumulation = 0.0f
                mDragDirection = 0

                // And trigger a continuous detecting.
                post(mHoldingMover)

                // IMPORTANT! Forgot this once. Fuck you man, fuck you!
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // End visual effect.
                view.setPressed(false)

                // Hide icon.
                getChildAt(4).visibility = GONE

                // Clear & Unlock.
                tileView.setTouchable(true)
                mDragger = null
                return true
            }

            MotionEvent.ACTION_MOVE ->                 //onMove(tileView, which, motionEvent);
                return true
        }

        return false
    }

    private fun moveImpl(draggerId: Int, distOnScreen: Float) {
        // The tileView we serve for.
        val tileView = mTileView?.get() ?: return

        // Amplify movement. Maybe we'd allow user to set it, or calculate based on screen
        // density and tileView's scale.
        val amp = 0.2f

        //Log.d(this, "" + distOnScreen + "," + mDragCurrentPos + "," + mDragBeginPosCorr);

        // If a previous round failed to move at least 1 oldBlock's wide,
        // it would accumulate the distance till in a future round we could move.
        val movement = distOnScreen * amp + mDragAccumulation

        // Translate the distance back to blocks count.
        val scale = tileView.scale
        val pxPerBlx = scale * MCTileProvider.TILESIZE / 16
        var distanceInBlocks = (movement / pxPerBlx).roundToInt()

        // If it's less than a oldBlock we couldn't move, let the accumulation grow.
        if (distanceInBlocks == 0) { //&& Math.abs(movement) >= 0.00001f) {
            mDragAccumulation = movement
            return
        }

        // Anti-jitter. If the user's scrolling right carefully, we don't want it suddenly goes
        // left because the view also moved and moved faster than the finger.
        if ( // If previous direction set, is negative to current, and current movement is short.
            (distanceInBlocks < 0 && mDragDirection > 0 && distOnScreen > -10.0f)
            || (distanceInBlocks > 0 && mDragDirection < 0 && distOnScreen < 10.0f)
        ) {
            // Then we do not move and clear accumulation.
            mDragAccumulation = 0f
            return
        }

        // Set direction for next round's anti-jitter.
        mDragDirection = if (distanceInBlocks > 0) 1 else -1

        // We've decided to move NOW AND TODAY clear accumulation.
        mDragAccumulation = 0f

        // Alter selection.
        // Selection shall be at least 1x1.
        when (draggerId) {
            R.id.left -> if (mSelectionRect.left + distanceInBlocks >= mSelectionRect.right) {
                mSelectionRect.left = mSelectionRect.right - 1
                distanceInBlocks = 0
            } else mSelectionRect.left += distanceInBlocks

            R.id.right -> if (mSelectionRect.right + distanceInBlocks <= mSelectionRect.left) {
                mSelectionRect.right = mSelectionRect.left + 1
                distanceInBlocks = 0
            } else mSelectionRect.right += distanceInBlocks

            R.id.top -> if (mSelectionRect.top + distanceInBlocks >= mSelectionRect.bottom) {
                mSelectionRect.top = mSelectionRect.bottom - 1
                distanceInBlocks = 0
            } else mSelectionRect.top += distanceInBlocks

            R.id.bottom -> if (mSelectionRect.bottom + distanceInBlocks <= mSelectionRect.top) {
                mSelectionRect.bottom = mSelectionRect.top + 1
                distanceInBlocks = 0
            } else mSelectionRect.bottom += distanceInBlocks
        }

        // If no movement, return.
        // It would be caused by the "Selection must be at least 1x1" rule.
        // For instance in case it's already 200x1 we can't move vertically.
        if (distanceInBlocks == 0) return

        // Notify outsides.
        mSelectionChangedListener?.onSelectionChanged(
            mSelectionRect
        )

        // Should we move the underlying tileView as well?
        // If touched point is near the moving direction (not the dragger position)
        // then we scroll.
        val sw = this.measuredWidth
        val sh = this.measuredHeight
        val minw = max(MIN_DIST_TO_SCREEN_BOUND, sw / 8)
        val minh = max(MIN_DIST_TO_SCREEN_BOUND, sh / 8)
        when (draggerId) {
            R.id.left, R.id.right ->                     // (Moving right and near right bound) or
                // (Moving left and near left bound)
                if (mDragDirection > 0 && sw - mDragCurrentPos < minw
                    || (mDragDirection < 0 && mDragCurrentPos < minw)
                ) tileView.scrollX = (tileView.scrollX + movement).toInt()
                else requestLayout()

            R.id.top, R.id.bottom -> if (mDragDirection > 0 && sh - mDragCurrentPos < minh
                || (mDragDirection < 0 && mDragCurrentPos < minh)
            ) tileView.scrollY = (tileView.scrollY + movement).toInt()
            else requestLayout()
        }
    }

    private fun onMove() {
        // If user no longer holding
        if (!isEnabled) return
        val dragger = mDragger ?: return

        // Get the view to retrieve its position.

        // Retrieve view position then get the distance and screen size.
        val draggerId = dragger.id

        // Distance between view position and current holding position.
        val distOnScreen: Float = when (draggerId) {
            R.id.left, R.id.right ->
                // `dragged.getX() - mDragBeginPosCorr` is the current position of user's initially
                // touched point of the View.
                mDragCurrentPos - dragger.x - mDragBeginPosCorr

            R.id.top, R.id.bottom -> mDragCurrentPos - dragger.y - mDragBeginPosCorr

            else -> return
        }

        this.moveImpl(draggerId, distOnScreen)

        // Schedule the next round.
        postDelayed(mHoldingMover, 40)
    }

    fun setTileView(tileView: MapTileView?) {
        mTileView = WeakReference<MapTileView?>(tileView)
    }

    fun beginSelection(selection: Rect) {
        mHasSelection = true
        mSelectionRect.set(selection)
        visibility = VISIBLE
        requestLayout()
    }

    fun beginSelection(centerX: Int, centerZ: Int) {
        // Requires this.
        val tileView: MapTileView = mTileView?.get() ?: return

        // Self's not visible now and not sure whether has measured dimensions.
        // Use tileView's instead.
        val scale = tileView.scale
        var w = tileView.measuredWidth
        val h = tileView.measuredHeight

        // Selection would be a square with length of half screen dimension.
        if (w > h) w = h / 4
        else w /= 4

        // Translate to blocks.
        val pxPerBlx = scale * MCTileProvider.TILESIZE / 16
        val rad = Math.round(w / pxPerBlx)
        mHasSelection = true

        // Set it.
        mSelectionRect.set(
            centerX - rad, centerZ - rad,
            centerX + rad, centerZ + rad
        )
        visibility = VISIBLE
        requestLayout()
    }

    fun onSelectionChangedOutsides(rect: Rect) {
        mSelectionRect.set(rect)
        // If the selection's far away from current viewport,
        // We want tho scroll the tileView to a nearest corner of the selection.
        mTileView?.get()?.let { tileView ->
            val pxPerBlx = tileView.scale * MCTileProvider.TILESIZE / 16
            val r = RectF()
            r.left = (rect.left + MCTileProvider.HALF_WORLDSIZE) * pxPerBlx
            r.top = (rect.top + MCTileProvider.HALF_WORLDSIZE) * pxPerBlx
            r.right = (rect.right + MCTileProvider.HALF_WORLDSIZE) * pxPerBlx
            r.bottom = (rect.bottom + MCTileProvider.HALF_WORLDSIZE) * pxPerBlx
            val scrollX = tileView.scrollX
            val scrollY = tileView.scrollY
            val sw = measuredWidth
            val sh = measuredHeight
            val viewport = RectF(
                scrollX.toFloat(),
                scrollY.toFloat(),
                (scrollX + sw).toFloat(),
                (scrollY + sh).toFloat()
            )
            if (!viewport.intersect(r)) {
                val hw = sw / 2.0f
                val currentX = scrollX + hw
                val hh = sh / 2.0f
                val currentY = scrollY + hh
                val dlt = ConvertUtil.distanceSq(currentX, currentY, r.left, r.top)
                val drt = ConvertUtil.distanceSq(currentX, currentY, r.right, r.top)
                val dlb = ConvertUtil.distanceSq(currentX, currentY, r.left, r.bottom)
                val drb = ConvertUtil.distanceSq(currentX, currentY, r.right, r.bottom)
                if (dlt < drt && dlt < dlb && dlt < drb) {
                    tileView.scrollX = (r.left - hw).toInt()
                    tileView.scrollY = (r.top - hh).toInt()
                } else if (drt < dlb && drt < drb) {
                    tileView.scrollX = (r.right - hw).toInt()
                    tileView.scrollY = (r.top - hh).toInt()
                } else if (dlb < drb) {
                    tileView.scrollX = (r.left - hw).toInt()
                    tileView.scrollY = (r.bottom - hh).toInt()
                } else {
                    tileView.scrollX = (r.right - hw).toInt()
                    tileView.scrollY = (r.bottom - hh).toInt()
                }
            }
        }
        requestLayout()
    }

    fun endSelection() {
        mHasSelection = false
        setVisibility(GONE)
        requestLayout()
    }

    fun hasSelection(): Boolean {
        return mHasSelection
    }


    val selection: Rect
        get() = Rect(mSelectionRect)

    override fun onLayout(b: Boolean, i: Int, i1: Int, i2: Int, i3: Int) {
        if (!mHasSelection) return

        val sw = getMeasuredWidth().toFloat()
        val sh = getMeasuredHeight().toFloat()

        val tileView = mTileView?.get() ?: return

        val scale = tileView.scale

        // Pixels per oldBlock.
        val pxPerBlx = scale * MCTileProvider.TILESIZE / 16

        // This would translate coordinate related to view, e.g. getScrollX() result,
        // into coordinate related to World.
        val halfWorld = pxPerBlx * MCTileProvider.HALF_WORLDSIZE

        // These translate selection dimensions (rel. to World) to screen.
        val transToScrX = tileView.scrollX.toFloat() - halfWorld
        val transToScrY = tileView.scrollY.toFloat() - halfWorld

        // Draw selected range
        mSelectionPixelRange.left = pxPerBlx * mSelectionRect.left - transToScrX
        mSelectionPixelRange.top = pxPerBlx * mSelectionRect.top - transToScrY
        mSelectionPixelRange.right = pxPerBlx * mSelectionRect.right - transToScrX
        mSelectionPixelRange.bottom = pxPerBlx * mSelectionRect.bottom - transToScrY

        // To the middle of lefter bound of on-screen selection area.
        var horix = ((if (mSelectionPixelRange.left < 0) 0.0f else mSelectionPixelRange.left)
                + (if (mSelectionPixelRange.right > sw) sw else mSelectionPixelRange.right)) / 2.0f
        var verty = ((if (mSelectionPixelRange.top < 0) 0.0f else mSelectionPixelRange.top)
                + (if (mSelectionPixelRange.bottom > sh) sh else mSelectionPixelRange.bottom)) / 2.0f

        // In case the selection's not too small.
        if (mSelectionPixelRange.right - mSelectionPixelRange.left > BUTTON_TO_BOUND_MIN_DIST * 2) {
            if (horix - mSelectionPixelRange.left < BUTTON_TO_BOUND_MIN_DIST) horix =
                mSelectionPixelRange.left + BUTTON_TO_BOUND_MIN_DIST

            if (mSelectionPixelRange.right - horix < BUTTON_TO_BOUND_MIN_DIST) horix =
                mSelectionPixelRange.right - BUTTON_TO_BOUND_MIN_DIST
        }
        if (mSelectionPixelRange.bottom - mSelectionPixelRange.top > BUTTON_TO_BOUND_MIN_DIST * 2) {
            if (verty - mSelectionPixelRange.top < BUTTON_TO_BOUND_MIN_DIST) verty =
                mSelectionPixelRange.top + BUTTON_TO_BOUND_MIN_DIST

            if (mSelectionPixelRange.bottom - verty < BUTTON_TO_BOUND_MIN_DIST) verty =
                mSelectionPixelRange.bottom - BUTTON_TO_BOUND_MIN_DIST
        }

        var horit = mSelectionPixelRange.top.toInt()
        var vertl = mSelectionPixelRange.left.toInt()
        var horib = mSelectionPixelRange.bottom.toInt()
        var vertr = mSelectionPixelRange.right.toInt()
        val horixi = horix.toInt()
        val vertyi = verty.toInt()

        // Let's prevent draggers collapse together when the selection's small.
        if (horib - horit <= MIN_DIST_DRAGGERS) {
            horit -= HALF_MIN_DIST_DRAGGERS
            horib += HALF_MIN_DIST_DRAGGERS
        }

        if (vertr - vertl <= MIN_DIST_DRAGGERS) {
            vertl -= HALF_MIN_DIST_DRAGGERS
            vertr += HALF_MIN_DIST_DRAGGERS
        }

        // Left
        var view = getChildAt(0)
        var humw = view.measuredWidth / 2
        var humh = view.measuredHeight / 2
        view.layout(vertl - humw, vertyi - humh, vertl + humw, vertyi + humh)

        // Top
        view = getChildAt(1)
        humw = view.measuredWidth / 2
        humh = view.measuredHeight / 2
        view.layout(horixi - humw, horit - humh, horixi + humw, horit + humh)

        // Right
        view = getChildAt(2)
        humw = view.measuredWidth / 2
        humh = view.measuredHeight / 2
        view.layout(vertr - humw, vertyi - humh, vertr + humw, vertyi + humh)

        // Top
        view = getChildAt(3)
        humw = view.measuredWidth / 2
        humh = view.measuredHeight / 2
        view.layout(horixi - humw, horib - humh, horixi + humw, horib + humh)

        // Current action icon.
        view = getChildAt(4)
        val vw = view.measuredWidth
        val vh = view.measuredHeight
        val l = (measuredWidth - vw) / 2
        val t = (measuredHeight - vh) / 2
        view.layout(l, t, l + vw, t + vh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!mHasSelection) return

        // Screen dimensions.
        val sw = measuredWidth.toFloat()
        val sh = measuredHeight.toFloat()

        if (!canvas.quickReject(mSelectionPixelRange, Canvas.EdgeType.BW)) {
            mVisiblePixelRange.left =
                if (mSelectionPixelRange.left < 0.0f) 0.0f else mSelectionPixelRange.left
            mVisiblePixelRange.top =
                if (mSelectionPixelRange.top < 0.0f) 0.0f else mSelectionPixelRange.top
            mVisiblePixelRange.right =
                if (mSelectionPixelRange.right > sw) sw else mSelectionPixelRange.right
            mVisiblePixelRange.bottom =
                if (mSelectionPixelRange.bottom > sh) sh else mSelectionPixelRange.bottom

            canvas.drawRect(mVisiblePixelRange, mPaint)
        }
    }

    fun setSelectionChangedListener(selectionChangedListener: SelectionChangedListener?) {
        mSelectionChangedListener = selectionChangedListener
    }
}
