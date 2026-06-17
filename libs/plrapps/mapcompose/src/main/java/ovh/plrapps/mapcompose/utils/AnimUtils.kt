package ovh.plrapps.mapcompose.utils

/**
 * Calculates a number between two numbers at a specific increment.
 */
fun lerp(start: Double, stop: Double, fraction: Float): Double {
    return start + (stop - start) * fraction
}
