package com.teamcheesecake.doomscrollpet.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

private const val CHANNEL_ID = "proximity"
// _v2: channel importance is fixed at creation and Android won't retroactively raise it for
// devices that already created "friend_scroll_alerts" at the old (DEFAULT) importance — a new
// id was the only way to actually get HIGH importance (heads-up) for existing installs.
private const val CHANNEL_SCROLL_ID = "friend_scroll_alerts_v2"

object ProximityNotifier {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nearby friends",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)

        val scrollChannel = NotificationChannel(
            CHANNEL_SCROLL_ID,
            "Friend scroll alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Lets you know when a friend has been doomscrolling"
        }
        manager.createNotificationChannel(scrollChannel)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Lint's MissingPermission check can't trace the guard through hasNotificationPermission()
    // — it's a real check, just not one Lint's flow analysis follows through a helper function.
    @SuppressLint("MissingPermission")
    fun notifyNearby(context: Context, friendName: String) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$friendName is nearby!")
            .setContentText("Go say hi — your pet loves it when you spend time together.")
            .setAutoCancel(true)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(friendName.hashCode(), notification)
    }

    /** Shown when a friend's device reports they've been doomscrolling an avoided app. */
    @SuppressLint("MissingPermission")
    fun notifyFriendScrolling(context: Context, friendName: String) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_SCROLL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$friendName needs a nudge")
            .setContentText("$friendName has been scrolling too much... they should go outside.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(("scroll_" + friendName).hashCode(), notification)
    }
}
