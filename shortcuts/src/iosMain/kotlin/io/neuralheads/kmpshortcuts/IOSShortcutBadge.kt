@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.neuralheads.kmpshortcuts

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNumber
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS implementation of [ShortcutBadge].
 *
 * Uses [UNUserNotificationCenter] (iOS 10+) to set the badge number.
 * This is the modern, recommended approach on iOS 16+ (replacing the deprecated
 * `UIApplication.shared.applicationIconBadgeNumber`).
 *
 * **Permissions:** Requires `NSUserNotificationUsageDescription` in `Info.plist`
 * and user approval via [requestPermission]. Call [requestPermission] once at app
 * startup before calling [setBadgeCount].
 *
 * ## Swift AppDelegate Setup
 * No additional AppDelegate setup is required. Just call [requestPermission] from
 * shared Kotlin code after `KMPShortcuts.initialize()`.
 *
 * ## Usage (shared Kotlin)
 * ```kotlin
 * // At startup:
 * val granted = KMPShortcuts.badge.requestPermission()
 *
 * // Update badge:
 * KMPShortcuts.badge.setBadgeCount(unreadMessages)
 *
 * // Clear:
 * KMPShortcuts.badge.clearBadge()
 * ```
 */
class IOSShortcutBadge : ShortcutBadge {

    private var _badgeCount = 0

    override val isBadgeSupported: Boolean = true

    override suspend fun requestPermission(): Boolean =
        suspendCancellableCoroutine { cont ->
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(UNAuthorizationOptionBadge) { granted, _ ->
                    cont.resume(granted)
                }
        }

    override suspend fun setBadgeCount(count: Int) {
        val safeCount = maxOf(0, count)
        _badgeCount = safeCount

        val center = UNUserNotificationCenter.currentNotificationCenter()
        val content = UNMutableNotificationContent().apply {
            setBadge(NSNumber(int = safeCount))
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = NOTIFICATION_ID,
            content    = content,
            trigger    = null
        )
        center.addNotificationRequest(request) { _ -> /* ignore error */ }
    }

    override fun getBadgeCount(): Int = _badgeCount

    companion object {
        private const val NOTIFICATION_ID = "kmpshortcuts.badge"
    }
}
