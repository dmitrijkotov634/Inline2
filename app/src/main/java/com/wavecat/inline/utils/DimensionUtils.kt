package com.wavecat.inline.utils

import android.content.res.Resources
import kotlin.math.roundToInt

/**
 * Converts density-independent pixels (dp) to pixels (px) for Int values.
 *
 * Extension property that converts the Int value from dp units to
 * pixel units using the device's display density. Commonly used
 * for setting view dimensions and margins in a density-independent way.
 *
 * @return Int The equivalent pixel value rounded to nearest integer
 * @see Resources.getSystem
 * @see kotlin.math.roundToInt
 */
val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

/**
 * Converts density-independent pixels (dp) to pixels (px) for Float values.
 *
 * Extension property that converts the Float value from dp units to
 * pixel units using the device's display density. Provides more precise
 * conversion for fractional dp values before rounding to integer pixels.
 *
 * @return Int The equivalent pixel value rounded to nearest integer
 * @see Resources.getSystem
 * @see kotlin.math.roundToInt
 */
val Float.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

/**
 * Converts pixels (px) to density-independent pixels (dp) for Int values.
 *
 * Extension property that converts the Int value from pixel units to
 * dp units using the device's display density. Useful for converting
 * measured pixel values back to density-independent units.
 *
 * @return Int The equivalent dp value as integer
 * @see Resources.getSystem
 */
val Int.px: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()