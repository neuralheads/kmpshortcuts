@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.neuralheads.kmpshortcuts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.UIMutableApplicationShortcutItem

/**
 * iOS implementation of [AppShortcutManager].
 *
 * ## How to apply shortcuts to UIApplication (Swift)
 *
 * In Kotlin/Native 2.0.21, UIApplication instance methods are not accessible
 * in a mixed Android+iOS KMP module. The library manages an in-memory shortcut
 * list. Apply it to UIApplication in your AppDelegate or main SwiftUI App struct:
 *
 * ```swift
 * import kmpshortcuts
 *
 * // In ApplicationMain or AppDelegate.applicationDidBecomeActive:
 * func applyKMPShortcuts() {
 *     let manager = KMPShortcuts.shared.manager as? IOSShortcutManager
 *     UIApplication.shared.shortcutItems = manager?.pendingShortcutItemsSnapshot
 * }
 *
 * // Handle taps:
 * func application(_ application: UIApplication,
 *     performActionFor shortcutItem: UIApplicationShortcutItem,
 *     completionHandler: @escaping (Bool) -> Void
 * ) {
 *     IOSShortcutManager.companion.handleShortcutItem(item: shortcutItem)
 *     completionHandler(true)
 * }
 * ```
 *
 * ## Platform Limits
 * iOS displays a maximum of 4 Home Screen Quick Actions.
 * [setShortcuts] silently trims to [maxShortcutCount] = 4.
 *
 * ## Pinning
 * iOS does not support pinned shortcuts. [requestPin] always returns `false`.
 */
class IOSShortcutManager : AppShortcutManager {

    private val mutex = Mutex()
    private var _cache: List<ShortcutInfo> = emptyList()

    override val maxShortcutCount: Int = 4

    // ── Mutations ──────────────────────────────────────────────────────────────

    override suspend fun setShortcuts(shortcuts: List<ShortcutInfo>): Unit =
        mutex.withLock { _cache = shortcuts.take(maxShortcutCount) }

    override suspend fun addShortcut(shortcut: ShortcutInfo): Unit =
        mutex.withLock { _cache = (_cache + shortcut).take(maxShortcutCount) }

    override suspend fun updateShortcut(id: String, update: ShortcutInfo.() -> ShortcutInfo): Unit =
        mutex.withLock {
            val idx = _cache.indexOfFirst { it.id == id }
            if (idx < 0) return@withLock
            _cache = _cache.toMutableList().also { it[idx] = it[idx].update() }
        }

    override suspend fun removeShortcut(id: String): Unit =
        mutex.withLock { _cache = _cache.filter { it.id != id } }

    override suspend fun clearShortcuts(): Unit =
        mutex.withLock { _cache = emptyList() }

    override suspend fun getShortcuts(): List<ShortcutInfo> =
        mutex.withLock { _cache.toList() }

    // ── Usage reporting ────────────────────────────────────────────────────────

    override suspend fun reportUsed(shortcutId: String): Unit =
        mutex.withLock {
            val target = _cache.firstOrNull { it.id == shortcutId } ?: return@withLock
            _cache = listOf(target) + _cache.filter { it.id != shortcutId }
        }

    // ── Pinning ────────────────────────────────────────────────────────────────

    override fun isPinSupported(): Boolean = false

    override suspend fun requestPin(shortcut: ShortcutInfo): Boolean = false

    // ── Activation ─────────────────────────────────────────────────────────────

    override fun observeActivations(): Flow<ShortcutActivationEvent> =
        _activationFlow.asSharedFlow()

    // ── Swift bridge ───────────────────────────────────────────────────────────

    /**
     * Non-suspend snapshot of current shortcuts as [UIMutableApplicationShortcutItem]s,
     * ready to assign to `UIApplication.shared.shortcutItems` from Swift.
     *
     * ```swift
     * UIApplication.shared.shortcutItems = manager.pendingShortcutItemsSnapshot
     * ```
     *
     * This property reads the in-memory cache directly. Since iOS shortcut item
     * assignment always happens on the main thread, this is safe for the
     * `applicationDidBecomeActive` use case.
     */
    val pendingShortcutItemsSnapshot: List<UIMutableApplicationShortcutItem>
        get() = _cache.map { it.toShortcutItem() }

    /**
     * Suspend version that reads the cache under the [Mutex].
     * Prefer [pendingShortcutItemsSnapshot] for synchronous access from Swift.
     */
    suspend fun pendingShortcutItems(): List<UIMutableApplicationShortcutItem> =
        mutex.withLock { _cache.map { it.toShortcutItem() } }

    companion object {
        private val _activationFlow = MutableSharedFlow<ShortcutActivationEvent>(
            extraBufferCapacity = 64,
        )

        /**
         * Route shortcut taps from AppDelegate into [observeActivations].
         *
         * ```swift
         * IOSShortcutManager.companion.handleShortcutItem(item: shortcutItem)
         * ```
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

private fun ShortcutInfo.toShortcutItem(): UIMutableApplicationShortcutItem =
    UIMutableApplicationShortcutItem(
        type              = id,
        localizedTitle    = shortLabel,
        localizedSubtitle = longLabel.takeIf { it != shortLabel },
        icon              = IOSShortcutIconResolver.resolve(icon),
        userInfo          = buildMap<Any?, Any?> {
            deepLinkAction?.let { put("deepLink", it) }
            putAll(extras)
        }
    )

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
