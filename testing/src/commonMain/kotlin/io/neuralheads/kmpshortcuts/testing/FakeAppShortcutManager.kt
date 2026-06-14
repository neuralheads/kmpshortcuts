package io.neuralheads.kmpshortcuts.testing

import io.neuralheads.kmpshortcuts.AppShortcutManager
import io.neuralheads.kmpshortcuts.ShortcutActivationEvent
import io.neuralheads.kmpshortcuts.ShortcutInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-memory [AppShortcutManager] implementation for unit tests.
 *
 * No platform dependencies — safe to use in commonTest, androidTest, or anywhere.
 *
 * ## Usage
 * ```kotlin
 * val fake = FakeAppShortcutManager()
 * val viewModel = ShortcutsViewModel(shortcuts = fake)
 *
 * viewModel.setup()
 * assertThat(fake.shortcuts).hasSize(2)
 *
 * // Simulate a shortcut tap:
 * fake.simulateTap("new_post")
 * ```
 */
class FakeAppShortcutManager(
    override val maxShortcutCount: Int = 4
) : AppShortcutManager {

    /** All current shortcuts. Inspect in tests. */
    val shortcuts = mutableListOf<ShortcutInfo>()

    /** All shortcut IDs passed to [reportUsed]. */
    val reportedUsage = mutableListOf<String>()

    /** All shortcuts passed to [requestPin]. */
    val pinRequests = mutableListOf<ShortcutInfo>()

    private val _activations = MutableSharedFlow<ShortcutActivationEvent>(
        extraBufferCapacity = 64
    )

    override suspend fun setShortcuts(shortcuts: List<ShortcutInfo>) {
        this.shortcuts.clear()
        this.shortcuts.addAll(shortcuts.take(maxShortcutCount))
    }

    override suspend fun addShortcut(shortcut: ShortcutInfo) {
        shortcuts.removeAll { it.id == shortcut.id }
        shortcuts.add(0, shortcut)
        while (shortcuts.size > maxShortcutCount) shortcuts.removeAt(shortcuts.size - 1)
    }

    override suspend fun updateShortcut(id: String, update: ShortcutInfo.() -> ShortcutInfo) {
        val idx = shortcuts.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        shortcuts[idx] = shortcuts[idx].update()
    }

    override suspend fun removeShortcut(id: String) {
        shortcuts.removeAll { it.id == id }
    }

    override suspend fun clearShortcuts() {
        shortcuts.clear()
    }

    override suspend fun getShortcuts(): List<ShortcutInfo> = shortcuts.toList()

    override suspend fun reportUsed(shortcutId: String) {
        reportedUsage.add(shortcutId)
    }

    override fun isPinSupported(): Boolean = true

    override suspend fun requestPin(shortcut: ShortcutInfo): Boolean {
        pinRequests.add(shortcut)
        return true
    }

    override fun observeActivations(): Flow<ShortcutActivationEvent> =
        _activations.asSharedFlow()

    /**
     * Simulate a user tapping the shortcut with [shortcutId].
     * Emits a [ShortcutActivationEvent] into [observeActivations].
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
}
