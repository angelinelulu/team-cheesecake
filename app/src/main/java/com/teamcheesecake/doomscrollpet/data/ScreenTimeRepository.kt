package com.teamcheesecake.doomscrollpet.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

/** Reads real per-app foreground time via UsageStatsManager using UsageEvents. */
class ScreenTimeRepository(private val context: Context) {

    @Suppress("DEPRECATION")
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Accurate screen-on minutes spent in foreground today for the specified packages. */
    fun getTodayUsageMinutes(packageNames: Collection<String>): Map<String, Long> {
        if (!hasUsageAccess() || packageNames.isEmpty()) return emptyMap()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(startOfDay, now)
        val event = UsageEvents.Event()

        val totalTimesMs = mutableMapOf<String, Long>()
        val startTimesMs = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (!packageNames.contains(pkg)) continue

            val isForeground = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            val isBackground = event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND

            if (isForeground) {
                if (!startTimesMs.containsKey(pkg)) {
                    startTimesMs[pkg] = event.timeStamp
                }
            } else if (isBackground) {
                // If app was already open across midnight, calculate duration from startOfDay
                val startTime = startTimesMs.remove(pkg) ?: startOfDay
                val duration = (event.timeStamp - startTime).coerceAtLeast(0L)
                totalTimesMs[pkg] = (totalTimesMs[pkg] ?: 0L) + duration
            }
        }

        // Account for an app currently open in foreground right now
        for ((pkg, startTime) in startTimesMs) {
            val duration = (now - startTime).coerceAtLeast(0L)
            totalTimesMs[pkg] = (totalTimesMs[pkg] ?: 0L) + duration
        }

        // Convert total milliseconds into minutes for each package
        return packageNames.associateWith { pkg ->
            (totalTimesMs[pkg] ?: 0L) / 60_000L
        }
    }

    /**
     * Whatever package is currently in the foreground, and how long (in ms) it's been there
     * *continuously* — i.e. since the last time it was resumed, not a cumulative daily total.
     * Used for near-real-time "you've been in this app for N seconds" checks, as opposed to
     * [getTodayUsageMinutes]'s whole-day totals. Returns null if usage access isn't granted or
     * nothing has been resumed within the lookback window (e.g. screen is off).
     */
    fun getCurrentForegroundSession(): ForegroundSession? {
        if (!hasUsageAccess()) return null

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - FOREGROUND_LOOKBACK_MS, now)
        val event = UsageEvents.Event()

        // Events are delivered in chronological order, so the last resume not yet followed by
        // a pause for that same package is whatever's on screen right now.
        var currentPackage: String? = null
        var currentSince = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    currentPackage = pkg
                    currentSince = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (currentPackage == pkg) currentPackage = null
                }
            }
        }

        val pkg = currentPackage ?: return null
        return ForegroundSession(packageName = pkg, sinceMillis = currentSince, dwellMs = now - currentSince)
    }

    data class ForegroundSession(val packageName: String, val sinceMillis: Long, val dwellMs: Long)

    companion object {
        private const val FOREGROUND_LOOKBACK_MS = 5 * 60 * 1000L
    }
}