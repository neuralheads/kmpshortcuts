package io.neuralheads.kmpshortcuts

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic interface for managing Home Screen / Launcher app shortcuts.
 *
 * Obtain the platform implementation via:
 * - Android: [AndroidShortcutManager]
 * - iOS:     [IOSShortcutManager]
 *
 * Register the implementation once at app startup:
 * ```kotlin
 * KMPShortcuts.initialize(AndroidShortcutManager(context))
 * ```
 *
 * ## Platform Limits
 * | Platform | Max dynamic shortcuts |
 * |----------|-----------------------|
 * | Android  | 5 (ShortcutManagerCompat limit) |
 * | iOS      | 4 total (static + dynamic) |
 *
 * Lists passed to [setShortcuts] are automatically trimmed to comply.
 */
interface AppShortcutManager {

    /**
     * Replace all dynamic shortcuts with [shortcuts].
     * List is trimmed to the platform max automatically (Android: 5, iOS: 4).
     */
    suspend fun setShortcuts(shortcuts: List<ShortcutInfo>)

    /**
     * Add a single shortcut. If the platform limit is reached, the oldest
     * shortcut is removed to make room (Android uses push semantics).
     */
    suspend fun addShortcut(shortcut: ShortcutInfo)

    /** Update an existing shortcut by [id]. No-op if [id] is not found. */
    suspend fun updateShortcut(id: String, update: ShortcutInfo.() -> ShortcutInfo)

    /** Remove shortcut by [id]. Silently ignores unknown IDs. */
    suspend fun removeShortcut(id: String)

    /** Remove all dynamic shortcuts. */
    suspend fun clearShortcuts()

    /** Returns all currently registered dynamic shortcuts. */
    suspend fun getShortcuts(): List<ShortcutInfo>

    /**
     * Report that [shortcutId] was used. Boosts its ranking on Android.
     * On iOS, re-inserts it at position 0 to approximate ranking.
     */
    suspend fun reportUsed(shortcutId: String)

    /**
     * Request the launcher to pin [shortcut] to the home screen.
     *
     * @return `true` if pinning is supported and the request was submitted;
     *         `false` on iOS (not supported) or if the launcher declined.
     */
    suspend fun requestPin(shortcut: ShortcutInfo): Boolean

    /** Whether the current platform/launcher supports pinned shortcuts. */
    fun isPinSupported(): Boolean

    /**
     * Hot [Flow] that emits a [ShortcutActivationEvent] each time the user
     * taps a shortcut and activates the app.
     *
     * - Android: feed via [AndroidShortcutManager.handleIntent] from `Activity.onCreate` / `onNewIntent`.
     * - iOS:     feed by calling `IOSShortcutManager.handleShortcutItem()`
     *            from AppDelegate.
     *
     * The flow uses `replay = 0` and `DROP_OLDEST` overflow so it never throws.
     */
    fun observeActivations(): Flow<ShortcutActivationEvent>

    /** Maximum number of dynamic shortcuts this platform allows. */
    val maxShortcutCount: Int
}
