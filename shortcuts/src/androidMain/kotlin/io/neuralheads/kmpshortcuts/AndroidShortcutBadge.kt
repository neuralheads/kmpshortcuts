package io.neuralheads.kmpshortcuts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Android implementation of [ShortcutBadge].
 *
 * Uses a **silent notification channel** to set the badge count via
 * [NotificationManagerCompat]. This is the most broadly compatible approach:
 *
 * - **Samsung One UI / Nova Launcher / etc.** — reads the notification count and shows it
 *   as a badge number on the launcher icon.
 * - **Pixel / AOSP** — shows a dot but not a number (this is a launcher limitation,
 *   not a library limitation).
 * - **No permission required** — silent notifications do not show in the shade.
 *
 * The channel is created lazily on first use and is permanently silenced:
 * `IMPORTANCE_MIN` + `setShowBadge(true)`.
 *
 * ## Setup
 * No additional setup required beyond `KMPShortcuts.initialize()`.
 * The badge is automatically available via `KMPShortcuts.badge`.
 */
class AndroidShortcutBadge(private val context: Context) : ShortcutBadge {

    private var _badgeCount = 0

    override val isBadgeSupported: Boolean = true

    override suspend fun requestPermission(): Boolean = true // No permission required on Android

    override suspend fun setBadgeCount(count: Int) {
        val safeCount = maxOf(0, count)
        _badgeCount = safeCount
        ensureChannel()

        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return // silently skip if notifications blocked globally

        if (safeCount == 0) {
            nm.cancel(NOTIFICATION_ID)
        } else {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setNumber(safeCount)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build()
            nm.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun getBadgeCount(): Int = _badgeCount

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Badge Counter",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Used to set the app icon badge count. No notifications are shown."
                setShowBadge(true)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID      = "kmpshortcuts_badge"
        private const val NOTIFICATION_ID = 0x4B4D5053 // "KMPS" in hex
    }
}
