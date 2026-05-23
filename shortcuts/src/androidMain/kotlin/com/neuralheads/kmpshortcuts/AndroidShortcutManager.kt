package com.neuralheads.kmpshortcuts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.neuralheads.kmpshortcuts.AppShortcutManager
import com.neuralheads.kmpshortcuts.ShortcutActivationEvent
import com.neuralheads.kmpshortcuts.ShortcutInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
            }
        }

    override suspend fun addShortcut(shortcut: ShortcutInfo): Unit =
        withContext(Dispatchers.Main) {
            mutex.withLock {
                ShortcutManagerCompat.pushDynamicShortcut(
                    context,
                    shortcut.toShortcutInfoCompat(context)
                )
            }
        }

    override suspend fun updateShortcut(id: String, update: ShortcutInfo.() -> ShortcutInfo): Unit {
        // Resolve the update outside nested lambdas to avoid Kotlin compiler
        // receiver-type-mismatch when a ShortcutInfo.()->ShortcutInfo lambda is
        // captured inside a suspend inline lambda chain (withContext + withLock).
        val existing = withContext(Dispatchers.Main) {
            mutex.withLock {
                ShortcutManagerCompat.getDynamicShortcuts(context).firstOrNull { it.id == id }
            }
        } ?: return
        val updated = with(existing.toShortcutInfo()) { update() }
        withContext(Dispatchers.Main) {
            mutex.withLock {
                ShortcutManagerCompat.updateShortcuts(
                    context,
                    listOf(updated.toShortcutInfoCompat(context))
                )
            }
        }
    }

    override suspend fun removeShortcut(id: String): Unit =
        withContext(Dispatchers.Main) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
        }

    override suspend fun clearShortcuts(): Unit =
        withContext(Dispatchers.Main) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        }

    override suspend fun getShortcuts(): List<ShortcutInfo> =
        withContext(Dispatchers.Main) {
            ShortcutManagerCompat.getDynamicShortcuts(context).map { it.toShortcutInfo() }
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

    // ── Companion ──────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "KMPShortcuts"

        private val _activationFlow = MutableSharedFlow<ShortcutActivationEvent>(
            extraBufferCapacity  = 64,
        )

        /**
         * Call from [android.app.Activity.onCreate] and [android.app.Activity.onNewIntent]
         * to route shortcut tap events into [AppShortcutManager.observeActivations].
         *
         * @return The [ShortcutActivationEvent] if the intent was a shortcut tap, else null.
         */
        fun handleIntent(intent: Intent): ShortcutActivationEvent? {
            val shortcutId = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID) ?: return null
            val deepLink   = intent.data?.toString()
            val extras     = intent.extras
                ?.keySet()
                ?.filter { it != Intent.EXTRA_SHORTCUT_ID }
                ?.associate { it to (intent.extras?.getString(it) ?: "") }
                ?: emptyMap()
            val event = ShortcutActivationEvent(
                shortcutId    = shortcutId,
                deepLinkAction = deepLink,
                extras        = extras
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
    intent.putExtra(Intent.EXTRA_SHORTCUT_ID, id)
    extras.forEach { (k, v) -> intent.putExtra(k, v) }

    val builder = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setRank(rank)
        .setIntent(intent)

    ShortcutIconResolver.resolve(icon, context)?.let { builder.setIcon(it) }

    return builder.build()
}

private fun ShortcutInfoCompat.toShortcutInfo(): ShortcutInfo = ShortcutInfo(
    id            = id,
    shortLabel    = shortLabel?.toString() ?: id,
    longLabel     = longLabel?.toString() ?: shortLabel?.toString() ?: id,
    rank          = rank,
    deepLinkAction = intent?.data?.toString()
)
