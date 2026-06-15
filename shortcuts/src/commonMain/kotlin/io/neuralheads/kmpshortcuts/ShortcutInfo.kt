package io.neuralheads.kmpshortcuts

/**
 * Describes a single app shortcut displayed on long-press of the app icon.
 *
 * ## Quick Start
 * ```kotlin
 * // Simple shortcut
 * ShortcutInfo(id = "search", shortLabel = "Search")
 *
 * // Full shortcut with deep link and icon
 * ShortcutInfo(
 *     id             = "new_post",
 *     shortLabel     = "New Post",
 *     longLabel      = "Create a new post",
 *     icon           = ShortcutIcon.System("square.and.pencil"),
 *     deepLinkAction = "myapp://new-post"
 * )
 *
 * // Conversation shortcut (Android share sheet integration)
 * ShortcutInfo(
 *     id         = "chat_alice",
 *     shortLabel = "Alice",
 *     icon       = ShortcutIcon.Resource("avatar_alice"),
 *     categories = setOf(ShortcutCategory.CONVERSATION),
 *     person     = ShortcutPerson(name = "Alice", key = "user_42")
 * )
 * ```
 *
 * Or use the [shortcut] DSL builder:
 * ```kotlin
 * val s = shortcut("new_post") {
 *     shortLabel = "New Post"
 *     icon       = ShortcutIcon.System("square.and.pencil")
 *     deepLink   = "myapp://new-post"
 * }
 * ```
 *
 * @param id Stable unique identifier. Used for updates, removal, and deep-link routing.
 *           Must be unique across all shortcuts.
 * @param shortLabel Concise label (~12 chars). Shown when space is limited (iOS).
 * @param longLabel Full label (~25 chars). Shown on Android when space allows.
 *                  Defaults to [shortLabel].
 * @param icon Icon to display. Use [ShortcutIcon] subclasses.
 * @param deepLinkAction URI string opened when shortcut is tapped (e.g. `"myapp://feed"`).
 *                       On Android, wrapped in an `Intent(ACTION_VIEW, Uri.parse(...))`.
 *                       On iOS, placed in `userInfo["deepLink"]`.
 * @param extras Arbitrary key→value payload delivered in [ShortcutActivationEvent.extras].
 * @param rank Display order hint. Lower = higher position. 0 is topmost.
 * @param categories Semantic tags for this shortcut. Used by Android to surface shortcuts
 *                   in the share sheet and notification shade. No-op on iOS.
 *                   See [ShortcutCategory].
 * @param person The real-world person associated with this shortcut. Required for full
 *               [ShortcutCategory.CONVERSATION] integration on Android. No-op on iOS.
 *               See [ShortcutPerson].
 */
data class ShortcutInfo(
    val id: String,
    val shortLabel: String,
    val longLabel: String = shortLabel,
    val icon: ShortcutIcon = ShortcutIcon.None,
    val deepLinkAction: String? = null,
    val extras: Map<String, String> = emptyMap(),
    val rank: Int = 0,
    val categories: Set<ShortcutCategory> = emptySet(),
    val person: ShortcutPerson? = null
)
