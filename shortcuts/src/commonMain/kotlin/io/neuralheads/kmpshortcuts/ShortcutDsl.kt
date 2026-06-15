package io.neuralheads.kmpshortcuts

/**
 * DSL builder for [ShortcutInfo].
 *
 * Create a single shortcut with the [shortcut] function:
 * ```kotlin
 * val s = shortcut("new_post") {
 *     shortLabel = "New Post"
 *     longLabel  = "Create a new post"
 *     icon       = ShortcutIcon.System("square.and.pencil")
 *     deepLink   = "myapp://new-post"
 *     rank       = 0
 * }
 * ```
 *
 * Create a list of shortcuts with the [shortcuts] function:
 * ```kotlin
 * val list = shortcuts {
 *     shortcut("new_post") {
 *         shortLabel = "New Post"
 *         icon       = ShortcutIcon.System("square.and.pencil")
 *         deepLink   = "myapp://new-post"
 *     }
 *     shortcut("search") {
 *         shortLabel = "Search"
 *         icon       = ShortcutIcon.System("magnifyingglass")
 *         deepLink   = "myapp://search"
 *     }
 * }
 * manager.setShortcuts(list)
 * ```
 */
@DslMarker
annotation class ShortcutDsl

/**
 * Builder class for [ShortcutInfo]. Use via the [shortcut] factory function.
 */
@ShortcutDsl
class ShortcutBuilder(val id: String) {
    /** Concise label (~12 chars). Required. */
    var shortLabel: String = id

    /**
     * Full label (~25 chars). Shown on Android when space allows.
     * Defaults to [shortLabel] if not set.
     */
    var longLabel: String? = null

    /** Icon for this shortcut. Defaults to [ShortcutIcon.None]. */
    var icon: ShortcutIcon = ShortcutIcon.None

    /**
     * Deep link URI opened when the shortcut is tapped.
     * Example: `"myapp://new-post"`
     */
    var deepLink: String? = null

    /**
     * Display order hint. Lower rank = higher position. 0 is topmost.
     * Default: 0.
     */
    var rank: Int = 0

    /**
     * Arbitrary key→value payload delivered in [ShortcutActivationEvent.extras]
     * when this shortcut is tapped.
     */
    var extras: Map<String, String> = emptyMap()

    /**
     * Semantic categories for this shortcut. Used by Android to surface
     * shortcuts in the share sheet and notification shade.
     * No-op on iOS. See [ShortcutCategory].
     */
    var categories: Set<ShortcutCategory> = emptySet()

    /**
     * Person associated with this shortcut. Required for full
     * [ShortcutCategory.CONVERSATION] integration on Android. No-op on iOS.
     */
    var person: ShortcutPerson? = null

    /** Build the final [ShortcutInfo]. */
    fun build(): ShortcutInfo = ShortcutInfo(
        id             = id,
        shortLabel     = shortLabel,
        longLabel      = longLabel ?: shortLabel,
        icon           = icon,
        deepLinkAction = deepLink,
        extras         = extras,
        rank           = rank,
        categories     = categories,
        person         = person
    )
}

/**
 * DSL builder for a [List] of [ShortcutInfo] items. Use via the [shortcuts] function.
 */
@ShortcutDsl
class ShortcutsBuilder {
    private val items = mutableListOf<ShortcutInfo>()

    /**
     * Add a shortcut to this list using the [ShortcutBuilder] DSL.
     * ```kotlin
     * shortcuts {
     *     shortcut("id") { shortLabel = "Label" }
     * }
     * ```
     */
    fun shortcut(id: String, block: ShortcutBuilder.() -> Unit = {}): ShortcutInfo {
        val item = ShortcutBuilder(id).apply(block).build()
        items.add(item)
        return item
    }

    /** Returns the built list. */
    fun build(): List<ShortcutInfo> = items.toList()
}

/**
 * Create a [ShortcutInfo] using a concise DSL.
 *
 * ```kotlin
 * val s = shortcut("new_post") {
 *     shortLabel = "New Post"
 *     longLabel  = "Create a new post"
 *     icon       = ShortcutIcon.System("square.and.pencil")
 *     deepLink   = "myapp://new-post"
 * }
 * ```
 *
 * @param id Stable unique identifier for this shortcut.
 * @param block Builder lambda to configure the shortcut properties.
 */
fun shortcut(id: String, block: ShortcutBuilder.() -> Unit = {}): ShortcutInfo =
    ShortcutBuilder(id).apply(block).build()

/**
 * Create a [List] of [ShortcutInfo] items using a concise DSL.
 *
 * ```kotlin
 * val list = shortcuts {
 *     shortcut("new_post") {
 *         shortLabel = "New Post"
 *         icon       = ShortcutIcon.System("square.and.pencil")
 *     }
 *     shortcut("search") {
 *         shortLabel = "Search"
 *         icon       = ShortcutIcon.System("magnifyingglass")
 *     }
 * }
 * manager.setShortcuts(list)
 * ```
 *
 * @param block Builder lambda to add shortcut items.
 */
fun shortcuts(block: ShortcutsBuilder.() -> Unit): List<ShortcutInfo> =
    ShortcutsBuilder().apply(block).build()
