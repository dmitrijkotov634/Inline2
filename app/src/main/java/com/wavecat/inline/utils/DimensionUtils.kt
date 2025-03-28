package com.wavecat.inline.utils

import android.content.res.Resources
import kotlin.math.roundToInt

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()
val Float.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()