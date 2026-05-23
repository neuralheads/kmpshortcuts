package com.neuralheads.kmpshortcuts

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

    private var _manager: AppShortcutManager? = null

    /**
     * Register the platform [AppShortcutManager] implementation.
     * Must be called before any access to [manager].
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
