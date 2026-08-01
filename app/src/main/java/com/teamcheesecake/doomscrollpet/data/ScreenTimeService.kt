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
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.teamcheesecake.doomscrollpet.FullScreenAlertActivity
import com.teamcheesecake.doomscrollpet.data.ScreenTimeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ScreenTimeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var screenTimeRepository: ScreenTimeRepository
    private var alertTriggeredToday = false

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
                    // Fetch user's current avoided apps from Firestore
                    val snapshot = db.collection("users").document(uid).get().await()
                    val avoidApps = (snapshot.get("avoidApps") as? List<*>)
                        ?.filterIsInstance<String>()?.toSet() ?: emptySet()

                    if (avoidApps.isNotEmpty()) {
                        val usageMinutes = screenTimeRepository.getTodayUsageMinutes(avoidApps).values.sum()

                        // Trigger full screen notification once usage hits 1 minute or more
                        if (usageMinutes >= 1L && !alertTriggeredToday) {
                            alertTriggeredToday = true
                            triggerFullScreenAlert()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Check every 10 seconds
                delay(10_000L)
            }
        }
        return START_STICKY
    }

    private fun triggerFullScreenAlert() {
        val intent = Intent(this, FullScreenAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Doomscroll Alert!")
            .setContentText("You've spent 1 minute on an avoided app.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Launches Full Screen Activity
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2002, builder.build())

        // Also directly launch activity for immediate takeover
        startActivity(intent)
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

            // Silent foreground service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Screen Time Service",
                NotificationManager.IMPORTANCE_LOW
            )

            // High importance alert channel for full screen intent
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Doomscroll Fullscreen Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows full screen warnings when doomscroll limits are hit"
            }

            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(alertChannel)
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
    }
}