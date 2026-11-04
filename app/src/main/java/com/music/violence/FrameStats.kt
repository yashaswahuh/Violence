package com.music.violence

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window

/**
 * Debug builds only: how long the app's frames take, logged every few seconds under the tag
 * `VioFrames`, so the Liquid Glass smoothness can be measured rather than judged by eye.
 *
 *     adb logcat -s VioFrames
 *
 * Each line is one window of frames: the count, the average and 95th / 99th percentile frame
 * time, and how many missed their deadline (`jank`). Scroll Home with Liquid Glass off, then on,
 * for the same few seconds each and compare the two. Release builds never attach this.
 */
object FrameStats {

    private const val TAG = "VioFrames"
    private const val WINDOW_MS = 5_000L

    private val durations = ArrayList<Long>(600)
    private var windowStart = 0L
    private var label: () -> String = { "" }

    /** Starts watching [activity]'s frames; [describe] says what is being measured (e.g. the glass setting). */
    fun attach(activity: Activity, describe: () -> String) {
        if (Build.VERSION.SDK_INT < 24) return
        if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        label = describe
        val window: Window = activity.window
        window.addOnFrameMetricsAvailableListener(
            { _, metrics, _ ->
                val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
                val now = System.nanoTime() / 1_000_000
                if (windowStart == 0L) windowStart = now
                durations.add(total)
                if (now - windowStart >= WINDOW_MS) report(activity)
            },
            Handler(Looper.getMainLooper()),
        )
    }

    private fun report(activity: Activity) {
        if (durations.size >= 10) {
            val sorted = durations.sorted()
            val refresh = if (Build.VERSION.SDK_INT >= 30) activity.display?.refreshRate ?: 60f else 60f
            val budgetNs = (1_000_000_000f / refresh).toLong()
            val jank = sorted.count { it > budgetNs }
            fun pct(p: Double) = sorted[((sorted.size - 1) * p).toInt()] / 1_000_000.0
            Log.i(
                TAG,
                "%s frames=%d avg=%.1fms p95=%.1fms p99=%.1fms jank=%d (%.0f%%) budget=%.1fms"
                    .format(
                        label(), sorted.size, sorted.average() / 1_000_000.0, pct(0.95), pct(0.99),
                        jank, 100.0 * jank / sorted.size, budgetNs / 1_000_000.0,
                    ),
            )
        }
        durations.clear()
        windowStart = 0L
    }
}
