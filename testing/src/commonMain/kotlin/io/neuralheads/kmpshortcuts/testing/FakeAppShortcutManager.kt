package io.neuralheads.kmpshortcuts.testing

import io.neuralheads.kmpshortcuts.AppShortcutManager
import io.neuralheads.kmpshortcuts.ShortcutActivationEvent
import io.neuralheads.kmpshortcuts.ShortcutInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [AppShortcutManager] implementation for unit tests.
 *
 * No platform dependencies — safe to use in `commonTest`, `androidTest`, or any JVM test.
 *
 * ## Basic Usage
 * ```kotlin
 * val fake = FakeAppShortcutManager()
 *
 * fake.setShortcuts(listOf(ShortcutInfo("new_post", "New Post")))
 * assertEquals(1, fake.shortcuts.size)
 * assertEquals("new_post", fake.shortcuts[0].id)
 * ```
 *
 * ## Simulating a Shortcut Tap
 * ```kotlin
 * val events = mutableListOf<ShortcutActivationEvent>()
 * val job = launch(UnconfinedTestDispatcher()) {
 *     fake.observeActivations().collect { events.add(it) }
 * }
 * fake.simulateTap("new_post", deepLinkAction = "myapp://new-post")
 * assertEquals(1, events.size)
 * job.cancel()
 * ```
 *
 * ## Resetting Between Tests
 * ```kotlin
 * @AfterTest
 * fun tearDown() {
 *     fake.reset()
 *     KMPShortcuts.resetForTesting()
 * }
 * ```
 *
 * @param maxShortcutCount The platform limit to simulate. Defaults to 4 (iOS limit).
 *                         Use 5 to simulate Android behaviour.
 */
class FakeAppShortcutManager(
    maxShortcutCount: Int = 4
) : AppShortcutManager {

    /** The simulated platform shortcut limit. Can be changed mid-test. */
    override var maxShortcutCount: Int = maxShortcutCount

    /** All current shortcuts. Inspect in tests. */
    val shortcuts = mutableListOf<ShortcutInfo>()

    /** All shortcut IDs passed to [reportUsed], in call order. */
    val reportedUsage = mutableListOf<String>()

    /** All shortcuts passed to [requestPin], in call order. */
    val pinRequests = mutableListOf<ShortcutInfo>()

    /**
     * Whether [isPinSupported] returns `true`. Set to `false` to simulate
     * a launcher that does not support pinning.
     */
    var simulatePinSupported: Boolean = true

    private val _activations = MutableSharedFlow<ShortcutActivationEvent>(
        extraBufferCapacity = 64
    )
    private val _shortcutsFlow = MutableStateFlow<List<ShortcutInfo>>(emptyList())

    override suspend fun setShortcuts(shortcuts: List<ShortcutInfo>) {
        this.shortcuts.clear()
        this.shortcuts.addAll(shortcuts.take(maxShortcutCount))
        _shortcutsFlow.value = this.shortcuts.toList()
    }

    override suspend fun addShortcut(shortcut: ShortcutInfo) {
        shortcuts.removeAll { it.id == shortcut.id }
        shortcuts.add(0, shortcut)
        while (shortcuts.size > maxShortcutCount) shortcuts.removeAt(shortcuts.size - 1)
        _shortcutsFlow.value = shortcuts.toList()
    }

    override suspend fun updateShortcut(id: String, transform: ShortcutInfo.() -> ShortcutInfo) {
        val idx = shortcuts.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        shortcuts[idx] = shortcuts[idx].transform()
        _shortcutsFlow.value = shortcuts.toList()
    }

    override suspend fun removeShortcut(id: String) {
        shortcuts.removeAll { it.id == id }
        _shortcutsFlow.value = shortcuts.toList()
    }

    override suspend fun clearShortcuts() {
        shortcuts.clear()
        _shortcutsFlow.value = emptyList()
    }

    override suspend fun getShortcuts(): List<ShortcutInfo> = shortcuts.toList()

    override suspend fun reportUsed(shortcutId: String) {
        reportedUsage.add(shortcutId)
    }

    override fun isPinSupported(): Boolean = simulatePinSupported

    override suspend fun requestPin(shortcut: ShortcutInfo): Boolean {
        if (!simulatePinSupported) return false
        pinRequests.add(shortcut)
        return true
    }

    override fun observeActivations(): Flow<ShortcutActivationEvent> =
        _activations.asSharedFlow()

    override fun observeShortcuts(): StateFlow<List<ShortcutInfo>> =
        _shortcutsFlow.asStateFlow()

    /**
     * Simulate a user tapping the shortcut with [shortcutId].
     * Emits a [ShortcutActivationEvent] into [observeActivations].
     *
     * ```kotlin
     * fake.simulateTap("new_post", deepLinkAction = "myapp://new-post")
     * ```
     */
    fun simulateTap(
        shortcutId: String,
        deepLinkAction: String? = null,
        extras: Map<String, String> = emptyMap()
    ) {
        _activations.tryEmit(
            ShortcutActivationEvent(
                shortcutId     = shortcutId,
                deepLinkAction = deepLinkAction,
                extras         = extras
            )
        )
    }

    /**
     * Simulate a shortcut activation using a pre-built [ShortcutActivationEvent].
     * Use this overload when you already have an event object:
     * ```kotlin
     * val event = ShortcutActivationEvent("search", extras = mapOf("query" to "kotlin"))
     * fake.simulateActivation(event)
     * ```
     */
    fun simulateActivation(event: ShortcutActivationEvent) {
        _activations.tryEmit(event)
    }

    /**
     * Reset all recorded state: shortcuts, reported usage, and pin requests.
     *
     * Call this in `@AfterTest` / `@After` to ensure test isolation when reusing
     * the same fake across multiple test cases.
     *
     * ```kotlin
     * @AfterTest
     * fun tearDown() {
     *     fake.reset()
     * }
     * ```
     */
    fun reset() {
        shortcuts.clear()
        reportedUsage.clear()
        pinRequests.clear()
        _shortcutsFlow.value = emptyList()
    }
}
