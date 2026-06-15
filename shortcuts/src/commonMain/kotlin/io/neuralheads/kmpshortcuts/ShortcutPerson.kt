package io.neuralheads.kmpshortcuts

/**
 * Represents a real-world person associated with a [ShortcutInfo].
 *
 * Used primarily with [ShortcutCategory.CONVERSATION] shortcuts to give the platform
 * enough context to rank, deduplicate, and surface the shortcut in conversation-aware
 * system surfaces (Android share sheet, notification shade).
 *
 * **Platform support:**
 * | Field  | Android | iOS |
 * |--------|---------|-----|
 * | [name] | ✅ Displayed in share sheet | ❌ No-op |
 * | [key]  | ✅ Used for deduplication across reinstalls | ❌ No-op |
 * | [uri]  | ✅ Linked to a contacts `content://` URI | ❌ No-op |
 * | [isBot]| ✅ Marks automated senders | ❌ No-op |
 *
 * ## Minimal Usage
 * ```kotlin
 * ShortcutInfo(
 *     id = "chat_alice",
 *     shortLabel = "Alice",
 *     categories = setOf(ShortcutCategory.CONVERSATION),
 *     person = ShortcutPerson(name = "Alice")
 * )
 * ```
 *
 * ## Full Usage
 * ```kotlin
 * ShortcutPerson(
 *     name = "Alice Smith",
 *     key  = "user_42",            // stable cross-reinstall identifier
 *     uri  = "tel:+15551234567",   // or a contacts content URI
 *     isBot = false
 * )
 * ```
 *
 * @param name  Display name of the person. Shown in the Android share sheet.
 * @param key   Stable, unique identifier for this person. Used by Android to
 *              deduplicate shortcuts across reinstalls. Should be a user ID or
 *              similarly stable value.
 * @param uri   A URI that identifies the person, e.g. `"tel:+15551234567"` or
 *              a `content://contacts` URI. Used for system-level contact resolution.
 * @param isBot `true` if this person represents an automated sender (bot / AI).
 */
data class ShortcutPerson(
    val name: String,
    val key: String? = null,
    val uri: String? = null,
    val isBot: Boolean = false
)
