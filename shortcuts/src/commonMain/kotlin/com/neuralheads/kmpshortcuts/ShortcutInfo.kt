package com.neuralheads.kmpshortcuts


/**
 * Describes a single app shortcut displayed on long-press of the app icon.
 *
 * @param id Stable unique identifier. Used for updates, removal, and deep-link routing.
 *           Must be unique across all shortcuts.
 * @param shortLabel Concise label (~12 chars). Shown when space is limited (iOS).
 * @param longLabel Full label (~25 chars). Shown on Android when space allows.
 * @param icon Icon to display. Use [ShortcutIcon] factory functions.
 * @param deepLinkAction URI string opened when shortcut is tapped (e.g. `"myapp://feed"`).
 *                       On Android, wrapped in an `Intent(ACTION_VIEW, Uri.parse(...))`.
 *                       On iOS, placed in `userInfo["deepLink"]`.
 * @param extras Arbitrary key→value payload delivered in [ShortcutActivationEvent.extras].
 * @param rank Display order hint. Lower = higher position. 0 is topmost.
 */
data class ShortcutInfo(
    val id: String,
    val shortLabel: String,
    val longLabel: String = shortLabel,
    val icon: ShortcutIcon = ShortcutIcon.None,
    val deepLinkAction: String? = null,
    val extras: Map<String, String> = emptyMap(),
    val rank: Int = 0
)
