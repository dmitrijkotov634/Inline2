package com.wavecat.inline.utils

import android.os.Handler
import android.os.Looper

fun runOnUiThread(body: () -> Unit) = Handler(Looper.getMainLooper()).post { body() }