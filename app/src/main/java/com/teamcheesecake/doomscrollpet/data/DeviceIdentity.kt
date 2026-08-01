package com.teamcheesecake.doomscrollpet.data

import android.content.Context

/**
 * A short code identifying this install, generated once and persisted locally.
 * Friends share codes with each other to link up — no login required.
 */
object DeviceIdentity {
    private const val PREFS_NAME = "device_identity"
    private const val KEY_CODE = "friend_code"
    private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I

    fun getOrCreateCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_CODE, null)?.let { return it }

        val code = (1..6).map { CODE_CHARS.random() }.joinToString("")
        prefs.edit().putString(KEY_CODE, code).apply()
        return code
    }
}
