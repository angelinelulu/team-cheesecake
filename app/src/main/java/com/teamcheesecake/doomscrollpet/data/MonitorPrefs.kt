package com.teamcheesecake.doomscrollpet.data

import android.content.Context

/**
 * Small SharedPreferences bridge so DoomscrollMonitorService (its own process
 * lifecycle) can read what PetViewModel (in-memory) currently has configured,
 * without the two needing to talk to each other directly.
 */
object MonitorPrefs {
    private const val PREFS_NAME = "doomscroll_monitor_prefs"
    private const val KEY_AVOID_APPS = "avoid_apps"
    private const val KEY_LIMIT_MINUTES = "limit_minutes"
    private const val KEY_PENDING_HEALTH_PENALTY = "pending_health_penalty"
    private const val DEFAULT_LIMIT_MINUTES = 60

    fun setAvoidApps(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_AVOID_APPS, packages).apply()
    }

    fun getAvoidApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_AVOID_APPS, emptySet()) ?: emptySet()

    fun setLimitMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_LIMIT_MINUTES, minutes).apply()
    }

    fun getLimitMinutes(context: Context): Int =
        prefs(context).getInt(KEY_LIMIT_MINUTES, DEFAULT_LIMIT_MINUTES)

    /** Called by the service when the user chooses "keep scrolling" on the overlay. */
    fun addPendingHealthPenalty(context: Context, amount: Int) {
        val current = prefs(context).getInt(KEY_PENDING_HEALTH_PENALTY, 0)
        prefs(context).edit().putInt(KEY_PENDING_HEALTH_PENALTY, current + amount).apply()
    }

    /** Called by PetViewModel to collect and clear whatever penalty has accumulated. */
    fun takePendingHealthPenalty(context: Context): Int {
        val amount = prefs(context).getInt(KEY_PENDING_HEALTH_PENALTY, 0)
        if (amount != 0) prefs(context).edit().putInt(KEY_PENDING_HEALTH_PENALTY, 0).apply()
        return amount
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
