package com.teamcheesecake.doomscrollpet.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

/** Reads real per-app foreground time via UsageStatsManager. Requires the user to grant
 * "Usage access" for this app in system settings (a special permission, not a runtime dialog). */
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

    /** Minutes spent in the foreground today, per package, for the given packages. */
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

        val stats = usageStatsManager.queryAndAggregateUsageStats(startOfDay, now)
        return packageNames.associateWith { pkg -> (stats[pkg]?.totalTimeInForeground ?: 0L) / 60_000L }
    }
}
