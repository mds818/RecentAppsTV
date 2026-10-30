package com.websnake.recentstv.data

import android.os.SystemClock
import android.util.Log

/** Opt-in developer timings: no package names, files, network or background work. */
internal class LoadTrace private constructor(private val label: String) {
    private val start = SystemClock.elapsedRealtimeNanos()
    fun mark(stage: String) { Log.d(TAG, "$label id=$start stage=$stage ms=${(SystemClock.elapsedRealtimeNanos() - start) / 1000000.0}") }
    companion object {
        private const val TAG = "RecentsPerf"
        fun begin(label: String): LoadTrace? = if (Log.isLoggable(TAG, Log.DEBUG)) LoadTrace(label).also { it.mark("start") } else null
    }
}
