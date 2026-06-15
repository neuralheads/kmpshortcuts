package io.neuralheads.kmpshortcuts

/**
 * Semantic category tags applied to a [ShortcutInfo].
 *
 * Categories signal to the platform what kind of action a shortcut represents,
 * enabling enhanced system behaviours such as conversation prioritisation in
 * the Android notification shade and share sheet.
 *
 * **Platform support:**
 * | Category | Android | iOS |
 * |----------|---------|-----|
 * | [CONVERSATION] | ✅ Shown in share sheet, conversation ranking | ❌ Ignored |
 * | [MEDIA]        | ✅ Recognised by media-related system surfaces | ❌ Ignored |
 *
 * ## Usage
 * ```kotlin
 * ShortcutInfo(
 *     id = "chat_alice",
 *     shortLabel = "Alice",
 *     categories = setOf(ShortcutCategory.CONVERSATION),
 *     person = ShortcutPerson(name = "Alice", key = "user_alice")
 * )
 * ```
 */
enum class ShortcutCategory {

    /**
     * Marks this shortcut as representing a conversation (e.g. a chat thread or contact).
     *
     * On Android, conversation shortcuts:
     * - Appear in the system share sheet under "Direct share"
     * - Are eligible for conversation ranking in the notification shade
     * - Must be paired with a [ShortcutInfo.person] for full system integration
     *
     * Maps to `android.shortcut.conversation` on Android. No-op on iOS.
     */
    CONVERSATION,

    /**
     * Marks this shortcut as representing a media playback action
     * (e.g. "Play Liked Songs", "Resume Podcast").
     *
     * Maps to `android.shortcut.media` on Android. No-op on iOS.
     */
    MEDIA;

    /** The Android system string value for this category. */
    internal val androidValue: String get() = when (this) {
        CONVERSATION -> "android.shortcut.conversation"
        MEDIA        -> "android.shortcut.media"
    }
}
