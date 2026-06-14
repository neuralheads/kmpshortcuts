package io.neuralheads.kmpshortcuts.sample

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.neuralheads.kmpshortcuts.KMPShortcuts
import io.neuralheads.kmpshortcuts.AndroidShortcutManager
import io.neuralheads.kmpshortcuts.ShortcutInfo
import io.neuralheads.kmpshortcuts.ShortcutIcon
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var statusView: TextView
    private lateinit var setDefaultBtn: Button
    private lateinit var addCustomBtn: Button
    private lateinit var removeBtn: Button
    private lateinit var pinBtn: Button

    private val logBuilder = SpannableStringBuilder()

    companion object {
        private const val TAG = "KMPShortcuts-Sample"
        private const val COLOR_SUCCESS = Color.GREEN
        private const val COLOR_ERROR   = 0xFFFF4444.toInt()
        private const val COLOR_INFO    = 0xFFAAAAAA.toInt()
        private const val COLOR_HEADER  = 0xFF00CCFF.toInt()
        private const val COLOR_EVENT   = 0xFFFFFF00.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())

        log("═══════════════════════════════════", COLOR_HEADER)
        log("  KMPShortcuts System Test App", COLOR_HEADER)
        log("  Library: io.neuralheads:kmpshortcuts", COLOR_HEADER)
        log("═══════════════════════════════════", COLOR_HEADER)

        // Route launch intent
        val event = AndroidShortcutManager.handleIntent(intent)
        if (event != null) {
            log("Launched via shortcut: ${event.shortcutId} (deepLink=${event.deepLinkAction}, extras=${event.extras})", COLOR_EVENT)
        } else {
            log("Normal app launch (no shortcut intent)", COLOR_INFO)
        }

        // Start collecting shortcut taps
        lifecycleScope.launch {
            KMPShortcuts.manager.observeActivations().collect { event ->
                log("Shortcut ACTIVATED: ID='${event.shortcutId}' Action='${event.deepLinkAction}' Extras=${event.extras}", COLOR_EVENT)
            }
        }

        updateShortcutsDisplay()

        setDefaultBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    KMPShortcuts.manager.setShortcuts(listOf(
                        ShortcutInfo(id = "new_task", shortLabel = "New Task", icon = ShortcutIcon.System("plus"), deepLinkAction = "kmpshortcuts://new-task"),
                        ShortcutInfo(id = "run_tests", shortLabel = "Run Tests", icon = ShortcutIcon.System("checkmark"), deepLinkAction = "kmpshortcuts://run-tests"),
                        ShortcutInfo(id = "settings", shortLabel = "Settings", icon = ShortcutIcon.System("gear"), deepLinkAction = "kmpshortcuts://settings")
                    ))
                    log("Successfully set default shortcuts!", COLOR_SUCCESS)
                    updateShortcutsDisplay()
                } catch (e: Exception) {
                    log("Failed to set shortcuts: ${e.message}", COLOR_ERROR)
                }
            }
        }

        addCustomBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val custom = ShortcutInfo(
                        id = "custom_" + System.currentTimeMillis() % 10000,
                        shortLabel = "Custom",
                        icon = ShortcutIcon.System("magnifyingglass"),
                        deepLinkAction = "kmpshortcuts://custom",
                        extras = mapOf("created_at" to System.currentTimeMillis().toString())
                    )
                    KMPShortcuts.manager.addShortcut(custom)
                    log("Added custom shortcut: ${custom.id}", COLOR_SUCCESS)
                    updateShortcutsDisplay()
                } catch (e: Exception) {
                    log("Failed to add shortcut: ${e.message}", COLOR_ERROR)
                }
            }
        }

        removeBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    KMPShortcuts.manager.removeShortcut("run_tests")
                    log("Removed shortcut 'run_tests' (if it existed)", COLOR_SUCCESS)
                    updateShortcutsDisplay()
                } catch (e: Exception) {
                    log("Failed to remove shortcut: ${e.message}", COLOR_ERROR)
                }
            }
        }

        pinBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    if (KMPShortcuts.manager.isPinSupported()) {
                        val pinInfo = ShortcutInfo(
                            id = "pinned_shortcut",
                            shortLabel = "Pinned",
                            icon = ShortcutIcon.System("star"),
                            deepLinkAction = "kmpshortcuts://pinned"
                        )
                        val requested = KMPShortcuts.manager.requestPin(pinInfo)
                        log("Requested shortcut pinning (success=$requested)", COLOR_SUCCESS)
                    } else {
                        log("Launcher does not support pinned shortcuts!", COLOR_ERROR)
                    }
                } catch (e: Exception) {
                    log("Failed to request pin: ${e.message}", COLOR_ERROR)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val event = AndroidShortcutManager.handleIntent(intent)
        if (event != null) {
            log("onNewIntent: Shortcut tap received! ID='${event.shortcutId}' Action='${event.deepLinkAction}'", COLOR_EVENT)
        }
    }

    private fun updateShortcutsDisplay() {
        lifecycleScope.launch {
            val list = KMPShortcuts.manager.getShortcuts()
            statusView.text = "Registered Dynamic Shortcuts: ${list.size} / ${KMPShortcuts.manager.maxShortcutCount}\nPinning supported: ${KMPShortcuts.manager.isPinSupported()}"
            log("Current shortcuts: " + list.map { it.id }.toString(), COLOR_INFO)
        }
    }

    private fun log(message: String, color: Int = COLOR_INFO) {
        val start = logBuilder.length
        logBuilder.append(message).append("\n")
        logBuilder.setSpan(
            ForegroundColorSpan(color),
            start,
            logBuilder.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        runOnUiThread {
            logView.text = logBuilder
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
        Log.d(TAG, message)
    }

    private fun buildLayout(): View {
        val context = this

        scrollView = ScrollView(context).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusView = TextView(context).apply {
            text = "Loading..."
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        container.addView(statusView)

        setDefaultBtn = Button(context).apply {
            text = "Set Default Shortcuts"
            setBackgroundColor(Color.parseColor("#21262D"))
            setTextColor(Color.WHITE)
            textSize = 13f
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            layoutParams = params
        }
        container.addView(setDefaultBtn)

        addCustomBtn = Button(context).apply {
            text = "Add Custom Shortcut"
            setBackgroundColor(Color.parseColor("#21262D"))
            setTextColor(Color.WHITE)
            textSize = 13f
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            layoutParams = params
        }
        container.addView(addCustomBtn)

        removeBtn = Button(context).apply {
            text = "Remove 'run_tests'"
            setBackgroundColor(Color.parseColor("#21262D"))
            setTextColor(Color.WHITE)
            textSize = 13f
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            layoutParams = params
        }
        container.addView(removeBtn)

        pinBtn = Button(context).apply {
            text = "Request Pin"
            setBackgroundColor(Color.parseColor("#21262D"))
            setTextColor(Color.WHITE)
            textSize = 13f
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            layoutParams = params
        }
        container.addView(pinBtn)

        logView = TextView(context).apply {
            textSize = 11f
            setTextColor(COLOR_INFO)
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 16, 0, 16)
        }

        container.addView(logView)
        scrollView.addView(container)
        return scrollView
    }
}
