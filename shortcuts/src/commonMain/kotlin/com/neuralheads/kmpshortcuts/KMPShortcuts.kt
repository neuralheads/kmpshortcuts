package com.neuralheads.kmpshortcuts

import kotlin.concurrent.Volatile

/**
 * Global entry point for KMPShortcuts.
 *
 * Call [initialize] once during app startup, then access [manager] from
 * anywhere in shared code.
 *
 * ## Android
 * ```kotlin
 * // Application.onCreate()
 * KMPShortcuts.initialize(AndroidShortcutManager(context = this))
 * ```
 *
 * ## iOS (from AppDelegate via Kotlin)
 * ```kotlin
 * KMPShortcuts.initialize(IOSShortcutManager())
 * ```
 */
object KMPShortcuts {

    /**
     * Backing field declared `@Volatile` (`kotlin.concurrent.Volatile`) so that a
     * write from the main thread (Application.onCreate / AppDelegate) is immediately
     * visible to any background thread that subsequently reads [manager].
     *
     * `kotlin.concurrent.Volatile` is the KMP-safe equivalent of `@kotlin.jvm.Volatile`
     * and is available on all targets since Kotlin 1.8.20.
     */
    @Volatile
    private var _manager: AppShortcutManager? = null

    /**
     * Register the platform [AppShortcutManager] implementation.
     * Must be called exactly once, before any access to [manager].
     *
     * Re-initializing with a different implementation is allowed (e.g. in tests)
     * but is not recommended in production code.
     */
    fun initialize(manager: AppShortcutManager) {
        _manager = manager
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

    /** Returns `true` if [initialize] has been called. */
    val isInitialized: Boolean get() = _manager != null
}
