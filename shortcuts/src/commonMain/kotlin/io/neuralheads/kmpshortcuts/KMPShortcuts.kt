package io.neuralheads.kmpshortcuts

import kotlin.concurrent.Volatile

/**
 * Global entry point for KMPShortcuts.
 *
 * Call [initialize] once during app startup, then access [manager] from
 * anywhere in shared code.
 *
 * ## Android — `Application.onCreate()`
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         KMPShortcuts.initialize(
 *             manager = AndroidShortcutManager(this),
 *             badge   = AndroidShortcutBadge(this)
 *         )
 *     }
 * }
 * ```
 *
 * ## iOS — `AppDelegate.application(_:didFinishLaunchingWithOptions:)`
 * ```kotlin
 * KMPShortcuts.initialize(
 *     manager = IOSShortcutManager(),
 *     badge   = IOSShortcutBadge()
 * )
 * ```
 */
object KMPShortcuts {

    /**
     * `@Volatile` so a write from the main thread is immediately visible to any
     * background thread that subsequently reads [manager].
     */
    @Volatile
    private var _manager: AppShortcutManager? = null

    @Volatile
    private var _badge: ShortcutBadge? = null

    /**
     * Register the platform implementations.
     * Must be called exactly once before any access to [manager] or [badge].
     *
     * Re-initializing is allowed in tests — call [resetForTesting] between test cases
     * for clean isolation.
     *
     * @param manager Platform [AppShortcutManager] implementation.
     * @param badge   Optional [ShortcutBadge] implementation. If omitted, [badge] will
     *                throw [IllegalStateException] until supplied.
     */
    fun initialize(manager: AppShortcutManager, badge: ShortcutBadge? = null) {
        _manager = manager
        _badge   = badge
    }

    /**
     * The registered [AppShortcutManager].
     * @throws IllegalStateException if [initialize] has not been called.
     */
    val manager: AppShortcutManager
        get() = _manager
            ?: error(
                "KMPShortcuts.initialize() has not been called. " +
                "Call it from Application.onCreate() (Android) or AppDelegate (iOS)."
            )

    /**
     * The registered [ShortcutBadge] implementation.
     *
     * @throws IllegalStateException if [initialize] has not been called, or if
     *         [initialize] was called without supplying a [badge] parameter.
     */
    val badge: ShortcutBadge
        get() = _badge
            ?: error(
                "No ShortcutBadge implementation registered. " +
                "Pass badge = AndroidShortcutBadge(context) / IOSShortcutBadge() " +
                "to KMPShortcuts.initialize()."
            )

    /** Returns `true` if [initialize] has been called. */
    val isInitialized: Boolean get() = _manager != null

    /**
     * Reset the singleton to its uninitialized state.
     *
     * **For use in unit tests ONLY.** Call this in `@AfterTest` / `@After` to ensure
     * clean state between test cases.
     *
     * ```kotlin
     * @AfterTest
     * fun tearDown() {
     *     KMPShortcuts.resetForTesting()
     * }
     * ```
     */
    fun resetForTesting() {
        _manager = null
        _badge   = null
    }
}
