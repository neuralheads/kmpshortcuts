package io.neuralheads.kmpshortcuts

import kotlinx.coroutines.flow.StateFlow

/**
 * A reactive, write-through wrapper around [AppShortcutManager] that exposes the
 * current shortcut list as a [StateFlow].
 *
 * `ShortcutStore` is the recommended way to interact with shortcuts from a ViewModel
 * or presenter because it gives you:
 * - **Immediate reactivity** — any mutation is instantly reflected in [shortcuts]
 * - **Snapshot access** — [shortcuts].value gives the current list without a suspend call
 * - **Optional persistence** — implement [Persister] to survive process death
 *
 * ## Setup
 * ```kotlin
 * // In your DI graph / Application
 * val store = ShortcutStore.create(
 *     manager           = KMPShortcuts.manager,
 *     initialShortcuts  = loadFromDisk() // or emptyList() on first launch
 * )
 * ```
 *
 * ## ViewModel usage
 * ```kotlin
 * class HomeViewModel(private val store: ShortcutStore) : ViewModel() {
 *
 *     // Collect the live list — re-emits on every mutation
 *     val shortcuts = store.shortcuts
 *         .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
 *
 *     fun setup() {
 *         viewModelScope.launch {
 *             store.set(shortcuts {
 *                 shortcut("new_post") { shortLabel = "New Post" }
 *                 shortcut("search")   { shortLabel = "Search"   }
 *             })
 *         }
 *     }
 * }
 * ```
 *
 * ## Persistence
 * Supply a [Persister] to automatically save and restore shortcuts across cold launches:
 * ```kotlin
 * val store = ShortcutStore.create(
 *     manager  = KMPShortcuts.manager,
 *     persister = MySharedPreferencesPersister()
 * )
 * ```
 */
interface ShortcutStore {

    /**
     * Hot [StateFlow] of all currently registered shortcuts.
     * Emits the current list immediately on collection, then re-emits on every mutation.
     */
    val shortcuts: StateFlow<List<ShortcutInfo>>

    /** Replace all shortcuts. Trims to [AppShortcutManager.maxShortcutCount] automatically. */
    suspend fun set(shortcuts: List<ShortcutInfo>)

    /** Add a single shortcut. Evicts the oldest if at capacity. */
    suspend fun add(shortcut: ShortcutInfo)

    /** Mutate an existing shortcut by [id]. No-op if not found. */
    suspend fun update(id: String, transform: ShortcutInfo.() -> ShortcutInfo)

    /** Remove a shortcut by [id]. Silently ignores unknown IDs. */
    suspend fun remove(id: String)

    /** Remove all shortcuts. */
    suspend fun clear()

    /**
     * Optional persistence hook. Implement this interface to save/restore the shortcut
     * list across process death (cold launches).
     *
     * The store calls [save] after every mutation and [restore] once during creation.
     *
     * ## Example (simple JSON-based persister)
     * ```kotlin
     * class JsonPersister(private val prefs: SharedPreferences) : ShortcutStore.Persister {
     *
     *     override suspend fun save(shortcuts: List<ShortcutInfo>) {
     *         val json = Json.encodeToString(shortcuts)
     *         prefs.edit().putString("shortcuts", json).apply()
     *     }
     *
     *     override suspend fun restore(): List<ShortcutInfo> {
     *         val json = prefs.getString("shortcuts", null) ?: return emptyList()
     *         return Json.decodeFromString(json)
     *     }
     * }
     * ```
     */
    interface Persister {
        /** Called after every mutation. Persist [shortcuts] to durable storage. */
        suspend fun save(shortcuts: List<ShortcutInfo>)

        /** Called once on store creation. Return the previously persisted list, or empty. */
        suspend fun restore(): List<ShortcutInfo>
    }

    companion object {
        /**
         * Create a [ShortcutStore] backed by the given [AppShortcutManager].
         *
         * @param manager          The platform manager to delegate mutations to.
         * @param initialShortcuts The initial shortcut list. Pass restored data from
         *                         [Persister.restore] here, or [emptyList] on first launch.
         * @param persister        Optional [Persister] for durable storage across cold launches.
         */
        fun create(
            manager: AppShortcutManager,
            initialShortcuts: List<ShortcutInfo> = emptyList(),
            persister: Persister? = null
        ): ShortcutStore = DefaultShortcutStore(manager, initialShortcuts, persister)
    }
}

/** Set shortcuts using the [shortcuts] DSL builder. */
suspend fun ShortcutStore.set(block: ShortcutsBuilder.() -> Unit) = set(shortcuts(block))

/** Add a shortcut using the [shortcut] DSL builder. */
suspend fun ShortcutStore.add(id: String, block: ShortcutBuilder.() -> Unit = {}) =
    add(shortcut(id, block))
