package io.neuralheads.kmpshortcuts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default implementation of [ShortcutStore].
 * Thread-safe via a [Mutex]. Delegates all platform mutations to [manager].
 */
internal class DefaultShortcutStore(
    private val manager: AppShortcutManager,
    initialShortcuts: List<ShortcutInfo>,
    private val persister: ShortcutStore.Persister?
) : ShortcutStore {

    private val mutex = Mutex()
    private val _shortcuts = MutableStateFlow(initialShortcuts)
    override val shortcuts: StateFlow<List<ShortcutInfo>> = _shortcuts.asStateFlow()

    override suspend fun set(shortcuts: List<ShortcutInfo>): Unit = mutex.withLock {
        manager.setShortcuts(shortcuts)
        _shortcuts.value = shortcuts.take(manager.maxShortcutCount)
        persister?.save(_shortcuts.value)
    }

    override suspend fun add(shortcut: ShortcutInfo): Unit = mutex.withLock {
        manager.addShortcut(shortcut)
        val current = _shortcuts.value.toMutableList()
        current.removeAll { it.id == shortcut.id }
        current.add(0, shortcut)
        while (current.size > manager.maxShortcutCount) current.removeAt(current.size - 1)
        _shortcuts.value = current
        persister?.save(_shortcuts.value)
    }

    override suspend fun update(id: String, transform: ShortcutInfo.() -> ShortcutInfo): Unit =
        mutex.withLock {
            manager.updateShortcut(id, transform)
            val idx = _shortcuts.value.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val updated = _shortcuts.value.toMutableList()
                updated[idx] = updated[idx].transform()
                _shortcuts.value = updated
                persister?.save(_shortcuts.value)
            }
        }

    override suspend fun remove(id: String): Unit = mutex.withLock {
        manager.removeShortcut(id)
        _shortcuts.value = _shortcuts.value.filter { it.id != id }
        persister?.save(_shortcuts.value)
    }

    override suspend fun clear(): Unit = mutex.withLock {
        manager.clearShortcuts()
        _shortcuts.value = emptyList()
        persister?.save(_shortcuts.value)
    }
}
