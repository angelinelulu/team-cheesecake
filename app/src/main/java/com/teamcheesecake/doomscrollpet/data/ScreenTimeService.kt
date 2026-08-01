package com.teamcheesecake.doomscrollpet.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.teamcheesecake.doomscrollpet.AppVisibilityTracker
import com.teamcheesecake.doomscrollpet.FullScreenAlertActivity
import com.teamcheesecake.doomscrollpet.screens.FullScreenRewardActivity
import com.teamcheesecake.doomscrollpet.data.ScreenTimeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenTimeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var screenTimeRepository: ScreenTimeRepository

    // Map display names to system package names
    private val appPackageMapping = mapOf(
        "YouTube" to "com.google.android.youtube",
        "Instagram" to "com.instagram.android",
        "TikTok" to "com.zhiliaoapp.musically",
        "Twitter" to "com.twitter.android",
        "X" to "com.twitter.android",
        "Facebook" to "com.facebook.katana",
        "Reddit" to "com.reddit.frontpage",
        "Snapchat" to "com.snapchat.android",
        // Good app mappings
        "Notes" to "com.google.android.keep",
        "Canvas" to "com.instructure.candroid",
        "Duolingo" to "com.duolingo",
        "Books" to "com.google.android.apps.books",
        "Fitness" to "com.google.android.apps.fitness",
        "Google" to "com.google.android.googlequicksearchbox"
    )

    private fun Set<String>.toPackageNames(): Set<String> {
        return this.map { appName ->
            appPackageMapping[appName] ?: appName
        }.toSet()
    }

    override fun onCreate() {
        super.onCreate()
        screenTimeRepository = ScreenTimeRepository(applicationContext)
        createNotificationChannels()
        startForeground(101, createPersistentNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val db = FirebaseFirestore.getInstance()

            while (isActive) {
                try {
                    val snapshot = db.collection("users").document(uid).get().await()
                    val rawAvoidApps = (snapshot.get("avoidApps") as? List<*>)
                        ?.filterIsInstance<String>()?.toSet() ?: emptySet()
                    val rawMoreApps = (snapshot.get("moreApps") as? List<*>)
                        ?.filterIsInstance<String>()?.toSet() ?: emptySet()

                    // Convert display names ("YouTube") to package names ("com.google.android.youtube")
                    val avoidApps = rawAvoidApps.toPackageNames()
                    val moreApps = rawMoreApps.toPackageNames()

                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val prefs = getSharedPreferences("snoot_alerts", Context.MODE_PRIVATE)

                    // Handle Avoid Apps
                    if (avoidApps.isNotEmpty()) {
                        val usageMinutes = screenTimeRepository.getTodayUsageMinutes(avoidApps).values.sum()
                        var lastAlertedMinutes = prefs.getLong("last_alerted_minutes", 0L)
                        val lastAlertDate = prefs.getString("last_alert_date", "")

                        // Reset counter on a new day
                        if (lastAlertDate != todayStr) {
                            lastAlertedMinutes = 0L
                            prefs.edit()
                                .putString("last_alert_date", todayStr)
                                .putLong("last_alerted_minutes", 0L)
                                .apply()
                        }

                        if (usageMinutes >= 1L && usageMinutes > lastAlertedMinutes) {
                            prefs.edit().putLong("last_alerted_minutes", usageMinutes).apply()
                            triggerAlertNotification(usageMinutes)
                        }
                    }

                    // Handle More Apps (Mirroring Reward System)
                    if (moreApps.isNotEmpty()) {
                        val usageMinutes = screenTimeRepository.getTodayUsageMinutes(moreApps).values.sum()
                        var lastRewardedMinutes = prefs.getLong("last_rewarded_minutes", 0L)
                        val lastRewardDate = prefs.getString("last_reward_date", "")

                        // Reset counter on a new day
                        if (lastRewardDate != todayStr) {
                            lastRewardedMinutes = 0L
                            prefs.edit()
                                .putString("last_reward_date", todayStr)
                                .putLong("last_rewarded_minutes", 0L)
                                .apply()
                        }

                        // Mirroring: reward every minute for simplicity, but could be adjusted
                        if (usageMinutes >= 1L && usageMinutes > lastRewardedMinutes) {
                            prefs.edit().putLong("last_rewarded_minutes", usageMinutes).apply()
                            triggerRewardNotification(usageMinutes)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(10_000L) // Checks screen time every 10 seconds
            }
        }
        return START_STICKY
    }

    private fun triggerAlertNotification(minutes: Long) {
        val intent = Intent(this, FullScreenAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            minutes.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Doomscroll Alert!")
            .setContentText("You've spent $minutes minute(s) on an avoided app today.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2002, builder.build())

        // Directly launch full screen alert over YouTube when overlay permission is granted
        if (Settings.canDrawOverlays(this)) {
            startActivity(intent)
        }
    }

    private fun triggerRewardNotification(minutes: Long) {
        val intent = Intent(this, FullScreenRewardActivity::class.java).apply {
            putExtra("minutes", minutes)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            minutes.toInt() + 10000, // Offset to avoid ID collision
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_REWARD_ID)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("Productivity Reward!")
            .setContentText("You've spent $minutes productive minute(s) today. Good job!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2003, builder.build())

        if (Settings.canDrawOverlays(this)) {
            startActivity(intent)
        }
    }

    private fun createPersistentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_SERVICE_ID)
            .setContentTitle("Snoot Screen Tracker")
            .setContentText("Monitoring screen time to keep your pet healthy.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Screen Time Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Doomscroll Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows warnings when doomscroll limits are hit"
            }

            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(alertChannel)
            val rewardChannel = NotificationChannel(
                CHANNEL_REWARD_ID,
                "Productivity Rewards",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows rewards when being productive"
            }
            manager?.createNotificationChannel(rewardChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_SERVICE_ID = "screen_time_service_channel"
        private const val CHANNEL_ALERT_ID = "screen_time_alert_channel"
        private const val CHANNEL_REWARD_ID = "screen_time_reward_channel"
    }
}