@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.neuralheads.kmpshortcuts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.UIMutableApplicationShortcutItem

/**
 * iOS implementation of [AppShortcutManager] backed by UIApplication shortcutItems.
 *
 * ## Setup in AppDelegate (Swift)
 * ```swift
 * import kmpshortcuts
 *
 * func application(
 *     _ application: UIApplication,
 *     performActionFor shortcutItem: UIApplicationShortcutItem,
 *     completionHandler: @escaping (Bool) -> Void
 * ) {
 *     IOSShortcutManagerKt.handleShortcutItem(shortcutItem)
 *     completionHandler(true)
 * }
 *
 * func application(_ application: UIApplication,
 *     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
 * ) -> Bool {
 *     if let item = launchOptions?[.shortcutItem] as? UIApplicationShortcutItem {
 *         IOSShortcutManagerKt.handleShortcutItem(item)
 *     }
 *     return true
 * }
 * ```
 *
 * ## Platform Limits
 * iOS displays a maximum of 4 Home Screen Quick Actions.
 * Lists passed to [setShortcuts] are trimmed to [maxShortcutCount] = 4.
 */
class IOSShortcutManager : AppShortcutManager {

    private val mutex = Mutex()

    // In-memory mirror of UIApplication.shortcutItems.
    // Kotlin/Native 2.0.x UIKit bindings don't expose shortcutItems as a gettable
    // property on UIApplication in mixed Android+iOS modules; we maintain our own state.
    private var _cache: List<ShortcutInfo> = emptyList()

    override val maxShortcutCount: Int = 4

    // ── Mutations ──────────────────────────────────────────────────────────────

    override suspend fun setShortcuts(shortcuts: List<ShortcutInfo>): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val trimmed = shortcuts.take(maxShortcutCount)
                _cache = trimmed
                applyToSystem(trimmed)
            }
        }

    override suspend fun addShortcut(shortcut: ShortcutInfo): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val updated = (_cache + shortcut).take(maxShortcutCount)
                _cache = updated
                applyToSystem(updated)
            }
        }

    override suspend fun updateShortcut(id: String, update: ShortcutInfo.() -> ShortcutInfo): Unit {
        val updated = mutex.withLock {
            val idx = _cache.indexOfFirst { it.id == id }
            if (idx < 0) return
            _cache.toMutableList().apply {
                this[idx] = with(this[idx]) { update() }
            }.toList()
        }
        withContext(Dispatchers.Main) {
            mutex.withLock {
                _cache = updated
                applyToSystem(updated)
            }
        }
    }

    override suspend fun removeShortcut(id: String): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val filtered = _cache.filter { it.id != id }
                _cache = filtered
                applyToSystem(filtered)
            }
        }

    override suspend fun clearShortcuts(): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                _cache = emptyList()
                applyToSystem(emptyList())
            }
        }

    override suspend fun getShortcuts(): List<ShortcutInfo> =
        mutex.withLock { _cache.toList() }

    // ── Usage reporting ────────────────────────────────────────────────────────

    override suspend fun reportUsed(shortcutId: String): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val target = _cache.firstOrNull { it.id == shortcutId } ?: return@withLock
                val reordered = listOf(target) + _cache.filter { it.id != shortcutId }
                _cache = reordered
                applyToSystem(reordered)
            }
        }

    // ── Pinning ────────────────────────────────────────────────────────────────

    override fun isPinSupported(): Boolean = false

    override suspend fun requestPin(shortcut: ShortcutInfo): Boolean = false

    // ── Activation ─────────────────────────────────────────────────────────────

    override fun observeActivations(): Flow<ShortcutActivationEvent> =
        _activationFlow.asSharedFlow()

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Applies the shortcut list to UIApplication via the ObjC `setShortcutItems:` message.
     * Must be called on the Main dispatcher.
     * We use [setShortcutItems] (the generated ObjC setter method) instead of the
     * `shortcutItems` property because Kotlin/Native 2.0.x UIKit bindings do not expose
     * the property setter in mixed Android+iOS KMP modules.
     */
    private fun applyToSystem(shortcuts: List<ShortcutInfo>) {
        UIApplication.sharedApplication.setShortcutItems(
            shortcuts.map { it.toShortcutItem() }
        )
    }

    companion object {
        private val _activationFlow = MutableSharedFlow<ShortcutActivationEvent>(
            extraBufferCapacity = 64,
        )

        /**
         * Route shortcut taps from AppDelegate into [observeActivations].
         */
        fun handleShortcutItem(item: UIApplicationShortcutItem) {
            val deepLink = item.userInfo?.get("deepLink")?.toString()
            val extras = item.userInfo
                ?.entries
                ?.filter { it.key != "deepLink" }
                ?.associate { it.key.toString() to it.value.toString() }
                ?: emptyMap()
            _activationFlow.tryEmit(
                ShortcutActivationEvent(
                    shortcutId     = item.type,
                    deepLinkAction = deepLink,
                    extras         = extras
                )
            )
        }
    }
}

// ── Mapping ────────────────────────────────────────────────────────────────────

private fun ShortcutInfo.toShortcutItem(): UIMutableApplicationShortcutItem {
    // Use the full ObjC initializer to avoid 'val cannot be reassigned' issues
    // that arise when Kotlin/Native maps UIMutableApplicationShortcutItem's
    // mutable properties as val (inherited read-only from UIApplicationShortcutItem).
    val userInfo = buildMap<Any?, Any?> {
        deepLinkAction?.let { put("deepLink", it) }
        putAll(extras)
    }
    return UIMutableApplicationShortcutItem(
        type              = id,
        localizedTitle    = shortLabel,
        localizedSubtitle = longLabel.takeIf { it != shortLabel },
        icon              = IOSShortcutIconResolver.resolve(icon),
        userInfo          = userInfo
    )
}

private fun UIApplicationShortcutItem.toShortcutInfo(): ShortcutInfo = ShortcutInfo(
    id             = type,
    shortLabel     = localizedTitle,
    longLabel      = localizedSubtitle ?: localizedTitle,
    deepLinkAction = userInfo?.get("deepLink")?.toString(),
    extras         = userInfo
        ?.entries
        ?.filter { it.key != "deepLink" }
        ?.associate { it.key.toString() to it.value.toString() }
        ?: emptyMap()
)
