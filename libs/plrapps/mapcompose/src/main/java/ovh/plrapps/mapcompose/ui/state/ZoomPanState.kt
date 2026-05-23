package ovh.plrapps.mapcompose.ui.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.core.GestureConfiguration
import ovh.plrapps.mapcompose.ui.layout.GestureListener
import ovh.plrapps.mapcompose.ui.layout.LayoutSizeChangeListener
import ovh.plrapps.mapcompose.utils.lerp
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

internal class ZoomPanState(
    private val stateChangeListener: ZoomPanStateListener,
    minScale: Double,
    maxScale: Double,
    scale: Double,
    gestureConfiguration: GestureConfiguration,
) : GestureListener, LayoutSizeChangeListener {
    private var scope: CoroutineScope? = null
    private var onLayoutContinuations = mutableListOf<Continuation<Unit>>()

    /**
     * Suspends until the view is laid out. To do that, we use the [scope] as flag.
     *
     * _Contract_:
     * On layout change, [scope] and [layoutSize] are initialized, and queued continuations
     * are resumed.
     */
    internal suspend fun awaitLayout() {
        if (scope != null) return
        suspendCoroutine {
            onLayoutContinuations.add(it)
        }
    }

    private val areGesturesEnabled by derivedStateOf { isScrollingEnabled || isZoomingEnabled }
    internal var isScrollingEnabled by mutableStateOf(true)
    internal var isZoomingEnabled by mutableStateOf(true)
    internal var isFlingZoomEnabled by mutableStateOf(true)

    /* Single source of truth. Don't mutate directly, use appropriate setScale(), etc. */
    internal var scale by mutableDoubleStateOf(scale)
    internal var cameraX by mutableDoubleStateOf(0.0)
    internal var cameraY by mutableDoubleStateOf(0.0)
    internal var layoutSize by mutableStateOf(IntSize.Zero)

    internal var visibleAreaPadding = VisibleAreaPadding(0, 0, 0, 0)

    var minScale = minScale
        set(value) {
            field = value.coerceAtLeast(Double.MIN_VALUE)
            setScale(scale)
        }

    var maxScale = maxScale
        set(value) {
            field = value
            setScale(scale)
        }

    // For user gestures animations
    private val userFloatAnimatable = Animatable(0f)
    private val userAnimatable: Animatable<Offset, AnimationVector2D> =
        Animatable(Offset.Zero, Offset.VectorConverter)

    // For api-based animations
    private val apiAnimatable = Animatable(0f)

    private val doubleTapSpec =
        TweenSpec<Float>(durationMillis = 300, easing = LinearOutSlowInEasing)
    private val flingZoomSpec =
        FloatExponentialDecaySpec(
            frictionMultiplier = gestureConfiguration.flingZoomFriction
        ).generateDecayAnimationSpec<Float>()

    fun setScale(scale: Double, notify: Boolean = true) {
        this.scale = constrainScale(scale)
        if (notify) notifyStateChanged()
    }

    fun setCamera(x: Double, y: Double) {
        this.cameraX = x
        this.cameraY = y
        notifyStateChanged()
    }

    /**
     * Scales the layout with animated scale, without maintaining scroll position.
     *
     * @param scale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    @Suppress("unused")
    suspend fun smoothScaleTo(
        scale: Double,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
    ): Boolean {
        return invokeAndCheckSuccess {
            val currScale = this@ZoomPanState.scale
            if (currScale > 0) {
                apiAnimatable.snapTo(0f)
                apiAnimatable.animateTo(1f, animationSpec) {
                    setScale(lerp(currScale, scale, value))
                }
            }
        }
    }

    /**
     * Animates the scroll to the destination value.
     *
     * @return `true` if the operation completed without being cancelled.
     */
    suspend fun smoothScrollTo(
        destCameraX: Double,
        destCameraY: Double,
        animationSpec: AnimationSpec<Float>
    ): Boolean {
        val startCameraX = this.cameraX
        val startCameraY = this.cameraY

        return invokeAndCheckSuccess {
            userAnimatable.stop()
            apiAnimatable.snapTo(0f)
            apiAnimatable.animateTo(1f, animationSpec) {
                setCamera(
                    x = lerp(startCameraX, destCameraX, value),
                    y = lerp(startCameraY, destCameraY, value)
                )
            }
        }
    }

    /**
     * Animates the scroll and the scale together with the supplied destination values.
     *
     * @param destCameraX Horizontal scroll of the destination point.
     * @param destCameraY Vertical scroll of the destination point.
     * @param destScale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    suspend fun smoothScrollScale(
        destCameraX: Double,
        destCameraY: Double,
        destScale: Double,
        animationSpec: AnimationSpec<Float>
    ): Boolean {
        val startCameraX = this.cameraX
        val startCameraY = this.cameraY
        val startScale = this.scale

        return invokeAndCheckSuccess {
            userAnimatable.stop()
            apiAnimatable.snapTo(0f)
            apiAnimatable.animateTo(1f, animationSpec) {
                setScale(lerp(startScale, destScale, value), false)
                setCamera(
                    x = lerp(startCameraX, destCameraX, value),
                    y = lerp(startCameraY, destCameraY, value)
                )
            }
        }
    }

    /**
     * Animates the layout to the scale provided, while maintaining position determined by the
     * the provided focal point.
     *
     * @param pivot The focal point to maintain, relative to the layout.
     * @param destScale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    private suspend fun smoothScaleWithFocalPoint(
        pivot: Offset,
        destScale: Double,
        animationSpec: AnimationSpec<Float>
    ): Boolean {
        val destScaleCst = constrainScale(destScale)
        val startScale = scale
        if (startScale == destScaleCst) return true
        /* Pinch and zoom magic */
        val offsetRatio = (destScaleCst - startScale) / (startScale * destScaleCst)
        return smoothScrollScale(
            (pivot.x - layoutSize.width / 2.0F) * offsetRatio + cameraX,
            (pivot.y - layoutSize.height / 2.0F) * offsetRatio + cameraY,
            destScaleCst,
            animationSpec
        )
    }

    /**
     * Invokes [block] in the scope of the composition and return whether the operation completed
     * without being cancelled.
     */
    internal suspend fun invokeAndCheckSuccess(block: suspend () -> Unit): Boolean {
        var success = true
        scope?.launch {
            block()
        }?.also {
            it.invokeOnCompletion { t ->
                if (t != null) success = false
            }
        }?.join()

        return success
    }

    suspend fun stopAnimations() {
        apiAnimatable.stop()
        userAnimatable.stop()
        userFloatAnimatable.stop()
    }

    override fun onScaleRatio(scaleRatio: Double, pivot: Offset) {
        if (!isZoomingEnabled) return

        val formerScale = scale
        setScale(formerScale * scaleRatio, false)

        /* Pinch and zoom magic */
        val offsetRatio = (scale - formerScale) / (formerScale * scale)
        setCamera(
            x = (pivot.x - layoutSize.width / 2.0F) * offsetRatio + cameraX,
            y = (pivot.y - layoutSize.height / 2.0F) * offsetRatio + cameraY
        )
    }

    override fun onScrollDelta(scrollDelta: Offset) {
        if (!isScrollingEnabled) return

        setCamera(cameraX - scrollDelta.x / scale, cameraY - scrollDelta.y / scale)
    }

    override fun onFling(flingSpec: DecayAnimationSpec<Offset>, velocity: Velocity) {
        if (!isScrollingEnabled) return

        scope?.launch {
            userAnimatable.snapTo(Offset.Zero)
            val initialCameraX = cameraX
            val initialCameraY = cameraY
            userAnimatable.animateDecay(
                initialVelocity = -Offset(velocity.x, velocity.y),
                animationSpec = flingSpec,
            ) {
                setCamera(
                    x = initialCameraX + value.x / scale,
                    y = initialCameraY + value.y / scale
                )
            }
        }
    }

    override fun onFlingZoom(velocity: Float, pivot: Offset) {
        if (!isZoomingEnabled || !isFlingZoomEnabled) return

        scope?.launch {
            userFloatAnimatable.snapTo(0f)
            var previous = 0f
            userFloatAnimatable.animateDecay(
                initialVelocity = velocity,
                animationSpec = flingZoomSpec,
            ) {
                /* Since scale = 2.pow(z - maxLevel)  , where z is the zoom level
                 * taking the derivative: d_scale = ln(2) * scale * d_z */
                val newScale = scale + ln(2.0) * scale * (value - previous)
                onScaleRatio(newScale / scale, pivot)
                previous = value
            }
        }
    }

    override fun onTouchDown() {
        if (!areGesturesEnabled) return

        scope?.launch {
            stopAnimations()
        }
        stateChangeListener.onTouchDown()
    }

    override fun onPress() {
        stateChangeListener.onPress()
    }

    override fun onTap(focalPt: Offset) {
        if (!stateChangeListener.detectsTap()) return
        stateChangeListener.onTap(focalPt.absoluteX(), focalPt.absoluteY())
    }

    override fun onLongPress(focalPt: Offset) {
        if (!stateChangeListener.detectsLongPress()) return
        stateChangeListener.onLongPress(focalPt.absoluteX(), focalPt.absoluteY())
    }

    fun Offset.absoluteX(
        scale: Double = this@ZoomPanState.scale
    ): Double = (x - layoutSize.width / 2.0F) / scale + cameraX

    fun Offset.absoluteY(
        scale: Double = this@ZoomPanState.scale
    ): Double = (y - layoutSize.height / 2.0F) / scale + cameraY

    override fun onDoubleTap(focalPt: Offset) {
        if (!isZoomingEnabled) return

        val destScale = 2.0.pow(floor(ln((scale * 2)) / ln(2.0)))

        scope?.launch {
            smoothScaleWithFocalPoint(
                focalPt,
                destScale,
                doubleTapSpec
            )
        }
    }

    override fun onTwoFingersTap(focalPt: Offset) {
        if (!isZoomingEnabled) return

        val destScale = 2.0.pow(floor(ln((scale / 2)) / ln(2.0)))

        scope?.launch {
            smoothScaleWithFocalPoint(
                focalPt,
                destScale,
                doubleTapSpec
            )
        }
    }

    override fun isListeningForGestures(): Boolean = areGesturesEnabled

    override fun shouldConsumeTapGesture(focalPt: Offset): Boolean {
        return stateChangeListener.interceptsTap(
            focalPt.x.toDouble(),
            focalPt.y.toDouble(),
            focalPt.absoluteX().toInt(),
            focalPt.absoluteY().toInt()
        )
    }

    override fun shouldConsumeLongPress(focalPt: Offset): Boolean {
        return stateChangeListener.interceptsLongPress(
            focalPt.x.toDouble(),
            focalPt.y.toDouble(),
            focalPt.absoluteX().toInt(),
            focalPt.absoluteY().toInt()
        )
    }

    override fun onSizeChanged(composableScope: CoroutineScope, size: IntSize) {
        scope = composableScope
        layoutSize = size

        /* Layout was done at least once, resume continuations */
        for (ct in onLayoutContinuations) {
            ct.resume(Unit)
        }
        onLayoutContinuations.clear()
    }

    internal fun constrainScale(scale: Double): Double {
        return scale.coerceIn(minScale, maxScale.coerceAtLeast(minScale))
    }

    private fun notifyStateChanged() {
        if (layoutSize != IntSize.Zero) {
            stateChangeListener.onStateChanged()
        }
    }

}

/**
 * The padding to apply when some UI is obscuring the map on it's borders.
 */
internal data class VisibleAreaPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

interface ZoomPanStateListener {
    fun onStateChanged()
    fun onTouchDown()
    fun onPress()
    fun onLongPress(x: Double, y: Double)
    fun onTap(x: Double, y: Double)
    fun detectsTap(): Boolean
    fun detectsLongPress(): Boolean
    fun interceptsTap(x: Double, y: Double, xPx: Int, yPx: Int): Boolean
    fun interceptsLongPress(x: Double, y: Double, xPx: Int, yPx: Int): Boolean
}