package com.wavecat.inline.utils

import android.os.Handler
import android.os.Looper

/**
 * Executes the given function on the main UI thread.
 *
 * Utility function that ensures code execution on the main thread,
 * which is required for UI operations in Android. Uses the main
 * looper to post the execution to the UI thread's message queue.
 *
 * @param body The function to execute on the UI thread
 * @author WaveCat
 * @see Handler
 * @see Looper.getMainLooper
 */
fun runOnUiThread(body: () -> Unit) = Handler(Looper.getMainLooper()).post { body() }