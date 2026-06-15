package io.neuralheads.kmpshortcuts

/**
 * Platform-agnostic API for managing the app icon **badge count** — the red
 * number shown on the app icon on the device home screen.
 *
 * ## Platform Support
 * | Feature | Android | iOS |
 * |---------|---------|-----|
 * | Set badge count | ✅ Via `ShortcutManagerCompat` notification channel | ✅ Via `UNUserNotificationCenter` |
 * | Get badge count | ✅ (cached value) | ✅ (cached value) |
 * | Clear badge | ✅ | ✅ |
 * | Permission required | ❌ None | ✅ Notification permission required on iOS 16+ |
 *
 * ## Android Notes
 * Android does not have a universal badge API. Badge support depends on the launcher:
 * - **Pixel / AOSP** — Supported via notification badge (no count shown by default).
 * - **Samsung One UI** — Full count badge supported.
 * - **Most modern launchers** — Show a dot or count derived from notification channels.
 *
 * KMPShortcuts uses `ShortcutManagerCompat` + a silent notification channel to set
 * the badge count, which is the most broadly compatible approach.
 *
 * ## iOS Notes
 * Requires the user to grant notification permissions. Call [requestPermission] before
 * calling [setBadgeCount] on iOS. On Android, [requestPermission] is a no-op (returns `true`).
 *
 * ## Usage
 * ```kotlin
 * // After initializing KMPShortcuts:
 * val badge = KMPShortcuts.badge
 *
 * // Show a badge with count 5
 * badge.setBadgeCount(5)
 *
 * // Clear the badge
 * badge.clearBadge()
 *
 * // Check if badges are supported
 * if (badge.isBadgeSupported) {
 *     badge.setBadgeCount(unreadCount)
 * }
 * ```
 */
interface ShortcutBadge {

    /**
     * Set the app icon badge count to [count].
     * - Pass `0` to clear the badge.
     * - Negative values are treated as `0`.
     *
     * On iOS, requires notification permission. Call [requestPermission] first.
     *
     * @param count The badge number to display. Use `0` to clear.
     */
    suspend fun setBadgeCount(count: Int)

    /**
     * Returns the last badge count set via [setBadgeCount], or `0` if the badge
     * has been cleared or was never set.
     *
     * Note: This reflects the value last set by this library, not necessarily
     * the value shown by the launcher (which may differ if modified externally).
     */
    fun getBadgeCount(): Int

    /**
     * Clear the app icon badge entirely (equivalent to `setBadgeCount(0)`).
     */
    suspend fun clearBadge() = setBadgeCount(0)

    /**
     * Request the OS permission required to show badge counts.
     *
     * - **Android**: Always returns `true` — no permission needed.
     * - **iOS**: Requests `UNAuthorizationOptions.badge`. Returns `true` if granted.
     *
     * @return `true` if permission was granted (or not required), `false` if denied.
     */
    suspend fun requestPermission(): Boolean

    /**
     * Whether the current platform and launcher combination supports badge counts.
     *
     * - **Android**: `true` on Samsung One UI and other launchers that support counts;
     *   `false` on AOSP launchers that only show a dot.
     * - **iOS**: Always `true` (all iOS launchers support count badges).
     */
    val isBadgeSupported: Boolean
}
