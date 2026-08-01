package com.teamcheesecake.doomscrollpet

object AppVisibilityTracker {
    @Volatile
    var isAppInForeground: Boolean = false
}