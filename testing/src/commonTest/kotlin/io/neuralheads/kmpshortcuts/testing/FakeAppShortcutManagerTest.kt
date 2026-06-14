package io.neuralheads.kmpshortcuts.testing

import io.neuralheads.kmpshortcuts.ShortcutActivationEvent
import io.neuralheads.kmpshortcuts.ShortcutInfo
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests that validate [FakeAppShortcutManager] itself.
 *
 * These tests ensure the test double behaves correctly so that any test that
 * *uses* the fake can rely on its semantics (capacity capping, deduplication,
 * activation event routing, etc.).
 */
class FakeAppShortcutManagerTest {

    private lateinit var fake: FakeAppShortcutManager

    @BeforeTest
    fun setUp() {
        fake = FakeAppShortcutManager(maxShortcutCount = 3)
    }

    // ── Capacity enforcement ───────────────────────────────────────────────────

    @Test
    fun `setShortcuts caps list to maxShortcutCount`() = runTest {
        fake.setShortcuts((1..5).map { s("s$it") })
        assertEquals(3, fake.shortcuts.size)
    }

    @Test
    fun `addShortcut caps at maxShortcutCount and removes last entry`() = runTest {
        fake.setShortcuts(listOf(s("a"), s("b"), s("c")))
        fake.addShortcut(s("d"))

        assertEquals(3, fake.shortcuts.size)
        // newest item lands at position 0, oldest evicted from the end
        assertEquals("d", fake.shortcuts[0].id)
        assertFalse(fake.shortcuts.any { it.id == "c" }, "oldest 'c' should have been evicted")
    }

    // ── Deduplication on addShortcut ───────────────────────────────────────────

    @Test
    fun `addShortcut replaces existing entry with same id`() = runTest {
        fake.addShortcut(s("dup", "Version 1"))
        fake.addShortcut(s("dup", "Version 2"))

        assertEquals(1, fake.shortcuts.size)
        assertEquals("Version 2", fake.shortcuts[0].shortLabel)
    }

    // ── updateShortcut ─────────────────────────────────────────────────────────

    @Test
    fun `updateShortcut mutates only matching entry`() = runTest {
        fake.setShortcuts(listOf(s("x"), s("y"), s("z")))
        fake.updateShortcut("y") { copy(shortLabel = "Updated Y") }

        assertEquals("x", fake.shortcuts[0].shortLabel)
        assertEquals("Updated Y", fake.shortcuts[1].shortLabel)
        assertEquals("z", fake.shortcuts[2].shortLabel)
    }

    @Test
    fun `updateShortcut with unknown id leaves shortcuts unchanged`() = runTest {
        fake.setShortcuts(listOf(s("only")))
        fake.updateShortcut("missing") { copy(shortLabel = "Ghost") }

        assertEquals(1, fake.shortcuts.size)
        assertEquals("only", fake.shortcuts[0].id)
    }

    // ── removeShortcut ─────────────────────────────────────────────────────────

    @Test
    fun `removeShortcut eliminates entry by id`() = runTest {
        fake.setShortcuts(listOf(s("keep"), s("delete_me")))
        fake.removeShortcut("delete_me")

        assertEquals(listOf("keep"), fake.shortcuts.map { it.id })
    }

    @Test
    fun `removeShortcut is idempotent for unknown id`() = runTest {
        fake.setShortcuts(listOf(s("solid")))
        fake.removeShortcut("ghost")
        fake.removeShortcut("ghost") // second call must also not throw

        assertEquals(1, fake.shortcuts.size)
    }

    // ── clearShortcuts ─────────────────────────────────────────────────────────

    @Test
    fun `clearShortcuts empties the shortcut list`() = runTest {
        fake.setShortcuts(listOf(s("a"), s("b")))
        fake.clearShortcuts()

        assertTrue(fake.shortcuts.isEmpty())
    }

    // ── reportUsed ─────────────────────────────────────────────────────────────

    @Test
    fun `reportUsed appends to reportedUsage in call order`() = runTest {
        fake.reportUsed("first")
        fake.reportUsed("second")
        fake.reportUsed("first") // duplicates are intentional (tracks call frequency)

        assertEquals(listOf("first", "second", "first"), fake.reportedUsage)
    }

    // ── pinning ────────────────────────────────────────────────────────────────

    @Test
    fun `requestPin returns true and records the shortcut`() = runTest {
        val shortcut = s("pin_target")
        val result = fake.requestPin(shortcut)

        assertTrue(result)
        assertEquals(listOf(shortcut), fake.pinRequests)
    }

    @Test
    fun `isPinSupported returns true from default fake`() {
        assertTrue(fake.isPinSupported())
    }

    // ── simulateTap and observeActivations ────────────────────────────────────

    @Test
    fun `simulateTap with minimal args emits event with correct shortcutId`() = runTest {
        val received = mutableListOf<ShortcutActivationEvent>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fake.observeActivations().collect { received.add(it) }
        }

        fake.simulateTap("my_action")

        assertEquals(1, received.size)
        assertEquals("my_action", received[0].shortcutId)
        assertTrue(received[0].extras.isEmpty())

        job.cancel()
    }

    @Test
    fun `simulateTap passes deepLinkAction and extras`() = runTest {
        val received = mutableListOf<ShortcutActivationEvent>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fake.observeActivations().collect { received.add(it) }
        }

        fake.simulateTap(
            shortcutId     = "deep_link_action",
            deepLinkAction = "myapp://home",
            extras         = mapOf("tab" to "feed")
        )

        val event = received.single()
        assertEquals("deep_link_action", event.shortcutId)
        assertEquals("myapp://home", event.deepLinkAction)
        assertEquals("feed", event.extras["tab"])

        job.cancel()
    }

    @Test
    fun `simulateTap fires multiple independent events in order`() = runTest {
        val ids = mutableListOf<String>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fake.observeActivations().collect { ids.add(it.shortcutId) }
        }

        listOf("one", "two", "three").forEach { fake.simulateTap(it) }

        assertEquals(listOf("one", "two", "three"), ids)
        job.cancel()
    }

    @Test
    fun `multiple collectors each receive emitted events`() = runTest {
        val first  = mutableListOf<String>()
        val second = mutableListOf<String>()

        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            fake.observeActivations().collect { first.add(it.shortcutId) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            fake.observeActivations().collect { second.add(it.shortcutId) }
        }

        fake.simulateTap("shared_event")

        assertEquals(listOf("shared_event"), first)
        assertEquals(listOf("shared_event"), second)

        job1.cancel()
        job2.cancel()
    }

    // ── getShortcuts returns snapshot ─────────────────────────────────────────

    @Test
    fun `getShortcuts snapshot is not affected by subsequent mutations`() = runTest {
        fake.setShortcuts(listOf(s("original")))
        val snapshot = fake.getShortcuts()

        fake.addShortcut(s("late_addition"))

        assertEquals(1, snapshot.size, "snapshot captured before mutation should remain stable")
        assertEquals(2, fake.getShortcuts().size)
    }

    // ── Custom maxShortcutCount ────────────────────────────────────────────────

    @Test
    fun `custom maxShortcutCount of 1 allows only one shortcut`() = runTest {
        val single = FakeAppShortcutManager(maxShortcutCount = 1)
        single.setShortcuts(listOf(s("a"), s("b"), s("c")))

        assertEquals(1, single.shortcuts.size)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun s(id: String, label: String = id): ShortcutInfo =
        ShortcutInfo(id = id, shortLabel = label)
}
