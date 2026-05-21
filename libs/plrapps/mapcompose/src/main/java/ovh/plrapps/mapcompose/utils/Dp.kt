package ovh.plrapps.mapcompose.utils

import android.content.res.Resources

@Deprecated("Use contextual density")
fun dpToPx(dp: Float): Float = dp * Resources.getSystem().displayMetrics.density