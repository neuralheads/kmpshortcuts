package io.neuralheads.kmpshortcuts

/**
 * Event emitted by [AppShortcutManager.observeActivations] when the user taps
 * a shortcut to open the app.
 *
 * @param shortcutId The [ShortcutInfo.id] of the tapped shortcut.
 * @param deepLinkAction The [ShortcutInfo.deepLinkAction] URI, if any.
 * @param extras The [ShortcutInfo.extras] map associated with the shortcut.
 */
data class ShortcutActivationEvent(
    val shortcutId: String,
    val deepLinkAction: String? = null,
    val extras: Map<String, String> = emptyMap()
)
