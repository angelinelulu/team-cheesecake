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
}