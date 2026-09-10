package com.waheed.artificerx.core.util

import android.util.Log

object DebugLogger {
    private const val TAG = "ArtificerX" // Logcat tag

    fun d(message: String) = Log.d(TAG, message)
    fun e(message: String, throwable: Throwable? = null) = Log.e(TAG, message, throwable)
    fun i(message: String) = Log.i(TAG, message)
}