package io.neuralheads.kmpshortcuts

/**
 * Extension functions for [AppShortcutManager] that provide convenient overloads
 * and utility helpers for common shortcut operations.
 *
 * These are available automatically wherever you import `io.neuralheads.kmpshortcuts`.
 */

/**
 * Replace all dynamic shortcuts using a vararg list.
 *
 * ```kotlin
 * manager.setShortcuts(
 *     shortcut("new_post") { shortLabel = "New Post" },
 *     shortcut("search")   { shortLabel = "Search" }
 * )
 * ```
 */
suspend fun AppShortcutManager.setShortcuts(vararg shortcuts: ShortcutInfo) =
    setShortcuts(shortcuts.toList())

/**
 * Add or update a shortcut. If a shortcut with the same [ShortcutInfo.id] already exists,
 * it is updated in-place. If it doesn't exist, it is added via [AppShortcutManager.addShortcut].
 *
 * ```kotlin
 * // Works whether "promo" already exists or not
 * manager.addOrUpdate(shortcut("promo") { shortLabel = "Flash Sale" })
 * ```
 */
suspend fun AppShortcutManager.addOrUpdate(shortcut: ShortcutInfo) {
    val existing = getShortcuts().firstOrNull { it.id == shortcut.id }
    if (existing != null) {
        updateShortcut(shortcut.id) { shortcut }
    } else {
        addShortcut(shortcut)
    }
}

/**
 * Returns `true` if the platform's shortcut limit has been reached, i.e.
 * [AppShortcutManager.getShortcuts] returns [AppShortcutManager.maxShortcutCount] or more items.
 *
 * ```kotlin
 * if (!manager.isAtCapacity()) {
 *     manager.addShortcut(newShortcut)
 * }
 * ```
 */
suspend fun AppShortcutManager.isAtCapacity(): Boolean =
    getShortcuts().size >= maxShortcutCount

/**
 * Remove multiple shortcuts by their IDs in a single call.
 *
 * ```kotlin
 * manager.removeShortcuts("old_promo", "debug_shortcut")
 * ```
 */
suspend fun AppShortcutManager.removeShortcuts(vararg ids: String) {
    ids.forEach { removeShortcut(it) }
}

/**
 * Set shortcuts using the [shortcuts] DSL builder.
 *
 * ```kotlin
 * manager.setShortcuts {
 *     shortcut("new_post") {
 *         shortLabel = "New Post"
 *         icon       = ShortcutIcon.System("square.and.pencil")
 *         deepLink   = "myapp://new-post"
 *     }
 *     shortcut("search") {
 *         shortLabel = "Search"
 *         icon       = ShortcutIcon.System("magnifyingglass")
 *     }
 * }
 * ```
 */
suspend fun AppShortcutManager.setShortcuts(block: ShortcutsBuilder.() -> Unit) =
    setShortcuts(shortcuts(block))
