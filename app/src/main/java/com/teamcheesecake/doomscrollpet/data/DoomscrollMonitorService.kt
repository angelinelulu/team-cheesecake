package com.teamcheesecake.doomscrollpet.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.teamcheesecake.doomscrollpet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 5_000L
private const val CONTINUOUS_ALERT_THRESHOLD_MS = 30_000L // 30 seconds, for demo purposes
private const val KEEP_SCROLLING_PENALTY = 10
private const val MONITOR_CHANNEL_ID = "doomscroll_monitor"
private const val ALERT_CHANNEL_ID = "doomscroll_alert"
private const val MONITOR_NOTIFICATION_ID = 1001
private const val ALERT_NOTIFICATION_ID = 1002

/**
 * Foreground service that watches which avoid-app the user is currently in (best-effort, via
 * the most recent foreground event — there's no direct "current app" API) and, once they've
 * been continuously in the same one past CONTINUOUS_ALERT_THRESHOLD_MS, shows a full-screen
 * overlay (needs "draw over other apps" permission) that blocks interaction until they pick
 * "close app" or "keep scrolling" (which docks pet health). Falls back to a heads-up
 * notification if that permission hasn't been granted, so something still happens either way.
 */
class DoomscrollMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private lateinit var screenTimeRepository: ScreenTimeRepository
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private var currentTrackedPackage: String? = null
    private var continuousStartTimeMillis: Long = 0L
    private var alertedForCurrentSession = false

    override fun onCreate() {
        super.onCreate()
        screenTimeRepository = ScreenTimeRepository(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ensureChannels()
        startForeground(MONITOR_NOTIFICATION_ID, buildMonitorNotification())
        scope.launch {
            while (true) {
                checkUsage()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun checkUsage() {
        if (!screenTimeRepository.hasUsageAccess()) return
        val avoidApps = MonitorPrefs.getAvoidApps(this)
        if (avoidApps.isEmpty()) return

        val foregroundPackage = screenTimeRepository.getCurrentForegroundPackage()
        val now = System.currentTimeMillis()

        if (foregroundPackage != null && avoidApps.contains(foregroundPackage)) {
            if (foregroundPackage != currentTrackedPackage) {
                currentTrackedPackage = foregroundPackage
                continuousStartTimeMillis = now
                alertedForCurrentSession = false
            }
            val elapsed = now - continuousStartTimeMillis
            if (elapsed >= CONTINUOUS_ALERT_THRESHOLD_MS && !alertedForCurrentSession) {
                triggerAlert()
                alertedForCurrentSession = true
            }
        } else {
            currentTrackedPackage = null
            alertedForCurrentSession = false
            removeOverlay()
        }
    }

    private fun triggerAlert() {
        if (Settings.canDrawOverlays(this)) {
            showOverlay()
        } else {
            sendFallbackNotification()
        }
    }

    // --- Overlay ---

    private fun showOverlay() {
        if (overlayView != null) return

        val view = LayoutInflater.from(this).inflate(R.layout.overlay_doomscroll_alert, null)

        view.findViewById<Button>(R.id.overlay_close_button).setOnClickListener {
            removeOverlay()
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }

        view.findViewById<Button>(R.id.overlay_continue_button).setOnClickListener {
            MonitorPrefs.addPendingHealthPenalty(this, KEEP_SCROLLING_PENALTY)
            removeOverlay()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0, // focusable + touchable + modal: blocks the app underneath until dismissed
            PixelFormat.TRANSLUCENT,
        )

        windowManager?.addView(view, params)
        overlayView = view
    }

    private fun removeOverlay() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }

    // --- Fallback notification (used only if overlay permission isn't granted) ---

    private fun sendFallbackNotification() {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Your pet is dying! 💀")
            .setContentText("You've been doomscrolling for 30+ seconds. Put the phone down!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        notifyIfAllowed(ALERT_NOTIFICATION_ID, notification)
    }

    private fun buildMonitorNotification(): Notification =
        NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("Doomscroll Pet is watching")
            .setContentText("Tracking time in apps you want to avoid.")
            .setOngoing(true)
            .build()

    private fun notifyIfAllowed(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        // Low importance: this is the persistent "watching" notification required to run the
        // foreground service — it shouldn't pop up or make noise every time it's shown.
        manager.createNotificationChannel(
            NotificationChannel(MONITOR_CHANNEL_ID, "Doomscroll monitoring", NotificationManager.IMPORTANCE_LOW)
        )

        // High importance fallback channel, only used if overlay permission isn't granted.
        manager.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL_ID, "Doomscroll alerts", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
