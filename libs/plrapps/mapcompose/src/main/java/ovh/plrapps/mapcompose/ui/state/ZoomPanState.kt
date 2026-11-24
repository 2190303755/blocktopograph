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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import ovh.plrapps.mapcompose.core.GestureConfiguration
import ovh.plrapps.mapcompose.utils.lerp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow

internal class ZoomPanState(
    private val stateChangeListener: () -> Unit,
    minScale: Double,
    maxScale: Double,
    scale: Double,
    gestureConfiguration: GestureConfiguration,
) {
    private val areGesturesEnabled by derivedStateOf { isScrollingEnabled || isZoomingEnabled }
    internal var isScrollingEnabled by mutableStateOf(true)
    internal var isZoomingEnabled by mutableStateOf(true)
    internal var isFlingZoomEnabled by mutableStateOf(true)

    /* Single source of truth. Don't mutate directly, use appropriate setScale(), etc. */
    internal var scale by mutableDoubleStateOf(scale)
    internal var cameraX by mutableDoubleStateOf(0.0)
    internal var cameraY by mutableDoubleStateOf(0.0)
    internal var layoutSize by mutableStateOf(IntSize.Zero)
        private set
    internal var visibleAreaPadding by mutableStateOf(IntOffset.Zero)
        private set

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
     * Scales with animation, without maintaining scroll position.
     * Requires [androidx.compose.runtime.MonotonicFrameClock].
     *
     * @param scale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    suspend fun smoothScaleTo(
        scale: Double,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
    ) {
        val currScale = this.scale
        if (currScale > 0) {
            apiAnimatable.snapTo(0f)
            apiAnimatable.animateTo(1f, animationSpec) {
                setScale(lerp(currScale, scale, value))
            }
        }
    }

    /**
     * Animates the scroll to the destination value.
     * Requires [androidx.compose.runtime.MonotonicFrameClock].
     */
    suspend fun smoothScrollTo(
        destCameraX: Double,
        destCameraY: Double,
        animationSpec: AnimationSpec<Float>
    ) {
        val startCameraX = this.cameraX
        val startCameraY = this.cameraY
        userAnimatable.stop()
        apiAnimatable.snapTo(0f)
        apiAnimatable.animateTo(1f, animationSpec) {
            setCamera(
                x = lerp(startCameraX, destCameraX, value),
                y = lerp(startCameraY, destCameraY, value),
            )
        }
    }

    /**
     * Animates the scroll and the scale together with the supplied destination values.
     * Requires [androidx.compose.runtime.MonotonicFrameClock].
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
    ) {
        val startCameraX = this.cameraX
        val startCameraY = this.cameraY
        val startScale = this.scale

        userAnimatable.stop()
        apiAnimatable.snapTo(0f)
        apiAnimatable.animateTo(1f, animationSpec) {
            setScale(lerp(startScale, destScale, value), false)
            setCamera(
                x = lerp(startCameraX, destCameraX, value),
                y = lerp(startCameraY, destCameraY, value),
            )
        }
    }

    /**
     * Animates the layout to the scale provided, while maintaining position determined by the
     * the provided focal point.
     * Requires [androidx.compose.runtime.MonotonicFrameClock].
     *
     * @param pivot The focal point to maintain, relative to the layout.
     * @param destScale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    private suspend fun smoothScaleWithFocalPoint(
        pivot: Offset,
        destScale: Double,
        animationSpec: AnimationSpec<Float>
    ) {
        val destScaleCst = constrainScale(destScale)
        val startScale = scale
        if (startScale == destScaleCst) return
        /* Pinch and zoom magic */
        val offsetRatio = (destScaleCst - startScale) / (startScale * destScaleCst)
        val size = layoutSize
        smoothScrollScale(
            (pivot.x - size.width / 2.0F) * offsetRatio + cameraX,
            (pivot.y - size.height / 2.0F) * offsetRatio + cameraY,
            destScaleCst,
            animationSpec
        )
    }

    suspend fun stopAnimations() {
        apiAnimatable.stop()
        userAnimatable.stop()
        userFloatAnimatable.stop()
    }

    fun onScaleRatio(scaleRatio: Double, pivot: Offset) {
        if (!isZoomingEnabled) return

        val formerScale = scale
        setScale(formerScale * scaleRatio, false)

        /* Pinch and zoom magic */
        val offsetRatio = (scale - formerScale) / (formerScale * scale)
        val size = layoutSize
        setCamera(
            x = (pivot.x - size.width / 2.0F) * offsetRatio + cameraX,
            y = (pivot.y - size.height / 2.0F) * offsetRatio + cameraY,
        )
    }

    fun onScrollDelta(scrollDelta: Offset) {
        if (!isScrollingEnabled) return

        setCamera(cameraX - scrollDelta.x / scale, cameraY - scrollDelta.y / scale)
    }

    suspend fun onFling(flingSpec: DecayAnimationSpec<Offset>, velocity: Velocity) {
        if (!isScrollingEnabled) return

        userAnimatable.snapTo(Offset.Zero)
        val initialCameraX = cameraX
        val initialCameraY = cameraY
        userAnimatable.animateDecay(
            initialVelocity = -Offset(velocity.x, velocity.y),
            animationSpec = flingSpec,
        ) {
            setCamera(
                x = initialCameraX + value.x / scale,
                y = initialCameraY + value.y / scale,
            )
        }
    }

    suspend fun onFlingZoom(velocity: Float, pivot: Offset) {
        if (!isZoomingEnabled || !isFlingZoomEnabled) return

        userFloatAnimatable.snapTo(0f)
        val ln2 = ln(2.0)
        var previous = 0f
        userFloatAnimatable.animateDecay(
            initialVelocity = velocity,
            animationSpec = flingZoomSpec,
        ) {
            /* Since scale = 2.pow(z - maxLevel)  , where z is the zoom level
             * taking the derivative: d_scale = ln(2) * scale * d_z */
            onScaleRatio(1.0 + ln2 * (value - previous), pivot)
            previous = value
        }
    }

    suspend fun onTouchDown() {
        if (!areGesturesEnabled) return

        stopAnimations()
    }

    fun absoluteX(offset: Offset): Double =
        (offset.x - layoutSize.width / 2.0F) / scale + cameraX

    fun absoluteY(offset: Offset): Double =
        (offset.y - layoutSize.height / 2.0F) / scale + cameraY

    suspend fun onDoubleTap(focalPt: Offset) {
        if (!isZoomingEnabled) return

        val destScale = 2.0.pow(floor(log2(scale * 2)))

        smoothScaleWithFocalPoint(
            focalPt,
            destScale,
            doubleTapSpec
        )
    }

    suspend fun onTwoFingersTap(focalPt: Offset) {
        if (!isZoomingEnabled) return

        val destScale = 2.0.pow(floor(log2(scale / 2)))

        smoothScaleWithFocalPoint(
            focalPt,
            destScale,
            doubleTapSpec
        )
    }

    fun isListeningForGestures(): Boolean = areGesturesEnabled

    /**
     * Called from the MeasurePolicy. Writes two independent layout states; they are kept separate
     * because size and visible-area offset change at different frequencies. Equal writes are
     * swallowed by `mutableStateOf` equality, so a pure offset lambda cannot create a re-measure
     * loop. Only a size change invalidates the tile viewport; offset changes are read directly by
     * the scroll APIs.
     */
    internal fun onMeasure(layoutSize: IntSize, visibleAreaPadding: IntOffset) {
        val sizeChanged = this.layoutSize != layoutSize
        this.layoutSize = layoutSize
        this.visibleAreaPadding = visibleAreaPadding
        if (sizeChanged) notifyStateChanged()
    }

    internal fun constrainScale(scale: Double): Double =
        scale.coerceIn(minScale, maxScale.coerceAtLeast(minScale))

    private fun notifyStateChanged() {
        if (layoutSize != IntSize.Zero) {
            stateChangeListener()
        }
    }
}
