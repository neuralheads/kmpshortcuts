package io.neuralheads.kmpshortcuts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Android implementation of [AppShortcutManager] backed by [ShortcutManagerCompat].
 *
 * ## Setup
 * ```kotlin
 * // Application.onCreate()
 * KMPShortcuts.initialize(AndroidShortcutManager(context = this))
 *
 * // MainActivity.kt
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     AndroidShortcutManager.handleIntent(intent)
 * }
 * override fun onNewIntent(intent: Intent) {
 *     super.onNewIntent(intent)
 *     AndroidShortcutManager.handleIntent(intent)
 * }
 * ```
 *
 * ## Pinning
 * Pinned shortcuts require a launcher that supports it (e.g. Pixel Launcher).
 * [isPinSupported] returns false on unsupported launchers.
 *
 * ## Limits
 * Android allows a maximum of [maxShortcutCount] dynamic shortcuts (typically 5).
 * Lists passed to [setShortcuts] are trimmed automatically.
 */
class AndroidShortcutManager(
    private val context: Context
) : AppShortcutManager {

    private val mutex = Mutex()
    private val _shortcutsFlow = MutableStateFlow<List<ShortcutInfo>>(emptyList())

    override val maxShortcutCount: Int
        get() = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)

    // ── Mutations ──────────────────────────────────────────────────────────────

    override suspend fun setShortcuts(shortcuts: List<ShortcutInfo>): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                val trimmed = shortcuts.take(maxShortcutCount)
                if (shortcuts.size > maxShortcutCount) {
                    Log.w(TAG, "setShortcuts: ${shortcuts.size} shortcuts provided but " +
                        "platform max is $maxShortcutCount. Trimming to $maxShortcutCount.")
                }
                ShortcutManagerCompat.setDynamicShortcuts(
                    context,
                    trimmed.map { it.toShortcutInfoCompat(context) }
                )
                _shortcutsFlow.value = trimmed
            }
        }

    override suspend fun addShortcut(shortcut: ShortcutInfo): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                ShortcutManagerCompat.pushDynamicShortcut(
                    context,
                    shortcut.toShortcutInfoCompat(context)
                )
                // Re-read from platform to stay in sync with push-eviction semantics
                _shortcutsFlow.value = ShortcutManagerCompat.getDynamicShortcuts(context)
                    .map { it.toShortcutInfoKMP() }
            }
        }

    override suspend fun updateShortcut(id: String, transform: ShortcutInfo.() -> ShortcutInfo): Unit {
        val existing = withContext(Dispatchers.Main) {
            mutex.withLock {
                ShortcutManagerCompat.getDynamicShortcuts(context).firstOrNull { it.id == id }
            }
        } ?: return
        val base: ShortcutInfo = existing.toShortcutInfoKMP()
        val updated: ShortcutInfo = transform(base)
        withContext(Dispatchers.Main) {
            mutex.withLock {
                ShortcutManagerCompat.updateShortcuts(
                    context,
                    listOf(updated.toShortcutInfoCompat(context))
                )
                val current = _shortcutsFlow.value.toMutableList()
                val idx = current.indexOfFirst { it.id == id }
                if (idx >= 0) { current[idx] = updated; _shortcutsFlow.value = current }
            }
        }
    }

    override suspend fun removeShortcut(id: String): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
                _shortcutsFlow.value = _shortcutsFlow.value.filter { it.id != id }
            }
        }

    override suspend fun clearShortcuts(): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                ShortcutManagerCompat.removeAllDynamicShortcuts(context)
                _shortcutsFlow.value = emptyList()
            }
        }

    override suspend fun getShortcuts(): List<ShortcutInfo> {
        val raw = withContext(Dispatchers.Main) {
            ShortcutManagerCompat.getDynamicShortcuts(context)
        }
        // Use toShortcutInfoKMP() which handles both ShortcutInfoCompat and
        // android.content.pm.ShortcutInfo (returned as a platform type on some
        // compileSdk / AGP combinations).
        return raw.map { it.toShortcutInfoKMP() }
    }

    // ── Usage reporting ────────────────────────────────────────────────────────

    override suspend fun reportUsed(shortcutId: String): Unit =
        withContext(Dispatchers.Main) {
            ShortcutManagerCompat.reportShortcutUsed(context, shortcutId)
        }

    // ── Pinning ────────────────────────────────────────────────────────────────

    override fun isPinSupported(): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    override suspend fun requestPin(shortcut: ShortcutInfo): Boolean =
        withContext(Dispatchers.Main) {
            if (!isPinSupported()) return@withContext false
            ShortcutManagerCompat.requestPinShortcut(
                context,
                shortcut.toShortcutInfoCompat(context),
                null
            )
        }

    // ── Activation ─────────────────────────────────────────────────────────────

    override fun observeActivations(): Flow<ShortcutActivationEvent> =
        _activationFlow.asSharedFlow()

    override fun observeShortcuts(): StateFlow<List<ShortcutInfo>> =
        _shortcutsFlow.asStateFlow()

    // ── Companion ──────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "KMPShortcuts"

        /** Key we use to embed the shortcut ID into the intent extras. */
        internal const val EXTRA_KMP_SHORTCUT_ID = "io.neuralheads.kmpshortcuts.SHORTCUT_ID"

        private val _activationFlow = MutableSharedFlow<ShortcutActivationEvent>(
            extraBufferCapacity = 64,
        )

        /**
         * Call from [android.app.Activity.onCreate] and [android.app.Activity.onNewIntent]
         * to route shortcut tap events into [AppShortcutManager.observeActivations].
         *
         * Checks multiple intent extras for maximum launcher compatibility:
         * 1. Our own custom extra (`EXTRA_KMP_SHORTCUT_ID`)
         * 2. The legacy `Intent.EXTRA_SHORTCUT_ID`
         * 3. Common launcher variant `"shortcut_id"`
         *
         * @return The [ShortcutActivationEvent] if the intent was a shortcut tap, else null.
         */
        fun handleIntent(intent: Intent): ShortcutActivationEvent? {
            val shortcutId = intent.getStringExtra(EXTRA_KMP_SHORTCUT_ID)
                ?: intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
                ?: intent.getStringExtra("shortcut_id")
                ?: return null
            val deepLink   = intent.data?.toString()
            val extras     = intent.extras
                ?.keySet()
                ?.filter { it != Intent.EXTRA_SHORTCUT_ID && it != EXTRA_KMP_SHORTCUT_ID && it != "shortcut_id" }
                ?.associate { it to (intent.extras?.getString(it) ?: "") }
                ?: emptyMap()
            val event = ShortcutActivationEvent(
                shortcutId     = shortcutId,
                deepLinkAction = deepLink,
                extras         = extras
            )
            _activationFlow.tryEmit(event)
            return event
        }
    }
}

// ── Mapping helpers ────────────────────────────────────────────────────────────

private fun ShortcutInfo.toShortcutInfoCompat(context: Context): ShortcutInfoCompat {
    val intent = deepLinkAction
        ?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
        ?: Intent(Intent.ACTION_VIEW).apply { setPackage(context.packageName) }
    // Use our own custom extra key for reliable round-tripping
    intent.putExtra(AndroidShortcutManager.EXTRA_KMP_SHORTCUT_ID, id)
    // Also set the legacy key for backwards compatibility
    intent.putExtra(Intent.EXTRA_SHORTCUT_ID, id)
    extras.forEach { (k, v) -> intent.putExtra(k, v) }

    val builder = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setRank(rank)
        .setIntent(intent)

    ShortcutIconResolver.resolve(icon, context)?.let { builder.setIcon(it) }

    // Conversation shortcuts — map ShortcutPerson → androidx.core.app.Person
    if (person != null) {
        val personBuilder = Person.Builder().setName(person.name)
        person.key?.let { personBuilder.setKey(it) }
        person.uri?.let  { personBuilder.setUri(it) }
        personBuilder.setBot(person.isBot)
        builder.setPerson(personBuilder.build())
    }

    // Map ShortcutCategory → platform string set
    if (categories.isNotEmpty()) {
        builder.setCategories(categories.map { it.androidValue }.toSet())
    }

    return builder.build()
}


/**
 * Maps any shortcut object back to a [ShortcutInfo].
 *
 * Handles both [ShortcutInfoCompat] (AndroidX wrapper) and the platform
 * [android.content.pm.ShortcutInfo] type that [ShortcutManagerCompat.getDynamicShortcuts]
 * may return depending on compileSdk / AGP version.
 *
 * **Extras round-trip**: all string extras stored in the backing intent are
 * re-hydrated into [ShortcutInfo.extras], excluding the internal
 * shortcut ID keys which are used only for routing.
 */
private val INTERNAL_EXTRA_KEYS = setOf(
    Intent.EXTRA_SHORTCUT_ID,
    AndroidShortcutManager.EXTRA_KMP_SHORTCUT_ID,
    "shortcut_id"
)

private fun Any.toShortcutInfoKMP(): ShortcutInfo {
    return when (this) {
        is ShortcutInfoCompat -> {
            val intent = intent
            val extras = intent?.extras
                ?.keySet()
                ?.filter { it !in INTERNAL_EXTRA_KEYS }
                ?.mapNotNull { key -> intent.extras?.getString(key)?.let { key to it } }
                ?.toMap()
                ?: emptyMap()
            ShortcutInfo(
                id             = id,
                shortLabel     = shortLabel?.toString() ?: id,
                longLabel      = longLabel?.toString() ?: shortLabel?.toString() ?: id,
                rank           = rank,
                deepLinkAction = intent?.data?.toString(),
                extras         = extras
            )
        }
        is android.content.pm.ShortcutInfo -> {
            val intent = intent
            val extras = intent?.extras
                ?.keySet()
                ?.filter { it !in INTERNAL_EXTRA_KEYS }
                ?.mapNotNull { key -> intent.extras?.getString(key)?.let { key to it } }
                ?.toMap()
                ?: emptyMap()
            ShortcutInfo(
                id             = id,
                shortLabel     = shortLabel?.toString() ?: id,
                longLabel      = longLabel?.toString() ?: shortLabel?.toString() ?: id,
                rank           = rank,
                deepLinkAction = intent?.data?.toString(),
                extras         = extras
            )
        }
        else -> error("Unexpected shortcut type: ${this::class}")
    }
}
