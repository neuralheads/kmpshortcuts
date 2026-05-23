@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.neuralheads.kmpshortcuts.ios

import com.neuralheads.kmpshortcuts.AppShortcutManager
import com.neuralheads.kmpshortcuts.ShortcutActivationEvent
import com.neuralheads.kmpshortcuts.ShortcutInfo
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
 * iOS implementation of [AppShortcutManager] backed by
 * `UIApplication.shared.shortcutItems`.
 *
 * ## Setup in AppDelegate (Swift)
 * ```swift
 * // AppDelegate.swift
 * import kmpshortcuts_ios
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
 * // Also handle cold-start shortcut tap:
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
 * iOS displays a maximum of **4** Home Screen Quick Actions (static + dynamic).
 * Lists passed to [setShortcuts] are trimmed to [maxShortcutCount] = 4.
 *
 * ## Pinning
 * iOS does not support pinned shortcuts. [requestPin] always returns `false`.
 */
class IOSShortcutManager : AppShortcutManager {

    private val mutex = Mutex()

    override val maxShortcutCount: Int = 4

    // ── Mutations ──────────────────────────────────────────────────────────────

    override suspend fun setShortcuts(shortcuts: List<ShortcutInfo>): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val trimmed = shortcuts.take(maxShortcutCount)
                UIApplication.sharedApplication.shortcutItems =
                    trimmed.map { it.toShortcutItem() }
            }
        }

    override suspend fun addShortcut(shortcut: ShortcutInfo): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val existing = currentItems()
                val updated  = (existing + shortcut).take(maxShortcutCount)
                UIApplication.sharedApplication.shortcutItems =
                    updated.map { it.toShortcutItem() }
            }
        }

    override suspend fun updateShortcut(id: String, update: ShortcutInfo.() -> ShortcutInfo): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val existing = currentItems()
                val updatedList = existing.map { if (it.id == id) it.update() else it }
                UIApplication.sharedApplication.shortcutItems =
                    updatedList.map { it.toShortcutItem() }
            }
        }

    override suspend fun removeShortcut(id: String): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val filtered = currentItems().filter { it.id != id }
                UIApplication.sharedApplication.shortcutItems =
                    filtered.map { it.toShortcutItem() }
            }
        }

    override suspend fun clearShortcuts(): Unit =
        withContext(Dispatchers.Main) {
            UIApplication.sharedApplication.shortcutItems = emptyList<Any?>()
        }

    override suspend fun getShortcuts(): List<ShortcutInfo> =
        withContext(Dispatchers.Main) { currentItems() }

    // ── Usage reporting ────────────────────────────────────────────────────────

    override suspend fun reportUsed(shortcutId: String): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                // iOS has no API equivalent — re-insert at index 0 to bump rank
                val items   = currentItems()
                val target  = items.firstOrNull { it.id == shortcutId } ?: return@withLock
                val reordered = listOf(target) + items.filter { it.id != shortcutId }
                UIApplication.sharedApplication.shortcutItems =
                    reordered.map { it.toShortcutItem() }
            }
        }

    // ── Pinning ────────────────────────────────────────────────────────────────

    override fun isPinSupported(): Boolean = false

    override suspend fun requestPin(shortcut: ShortcutInfo): Boolean = false

    // ── Activation ─────────────────────────────────────────────────────────────

    override fun observeActivations(): Flow<ShortcutActivationEvent> =
        _activationFlow.asSharedFlow()

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun currentItems(): List<ShortcutInfo> =
        UIApplication.sharedApplication.shortcutItems
            ?.mapNotNull { (it as? UIApplicationShortcutItem)?.toShortcutInfo() }
            ?: emptyList()

    companion object {
        private val _activationFlow = MutableSharedFlow<ShortcutActivationEvent>(
            extraBufferCapacity = 64,
        )

        /**
         * Call from AppDelegate's `performActionFor shortcutItem:` and
         * `didFinishLaunchingWithOptions` to route taps into [observeActivations].
         */
        fun handleShortcutItem(item: UIApplicationShortcutItem) {
            val deepLink = item.userInfo
                ?.get("deepLink")?.toString()
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
    val item = UIMutableApplicationShortcutItem(
        type           = id,
        localizedTitle = shortLabel
    )
    item.localizedSubtitle = longLabel.takeIf { it != shortLabel }
    item.icon = IOSShortcutIconResolver.resolve(icon)

    val userInfo = mutableMapOf<Any?, Any?>()
    deepLinkAction?.let { userInfo["deepLink"] = it }
    extras.forEach { (k, v) -> userInfo[k] = v }
    item.userInfo = userInfo

    return item
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
