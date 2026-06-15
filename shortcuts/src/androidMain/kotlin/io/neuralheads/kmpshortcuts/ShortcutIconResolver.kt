package io.neuralheads.kmpshortcuts

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Resolves a [ShortcutIcon] to an [IconCompat] suitable for [ShortcutInfoCompat].
 *
 * Fallback chain:
 * 1. Exact resource lookup by name
 * 2. System Material icon mapping
 * 3. Default compass icon (`android.R.drawable.ic_menu_compass`)
 */
internal object ShortcutIconResolver {

    private const val TAG = "KMPShortcuts"

    fun resolve(icon: ShortcutIcon, context: Context): IconCompat? = when (icon) {
        is ShortcutIcon.None -> null

        is ShortcutIcon.System -> {
            // Map SF Symbol name → Android drawable name heuristic
            val drawableName = MaterialSymbolMapper.map(icon.name)
            resolveDrawable(drawableName, context)
                ?: run {
                    Log.w(TAG, "System icon '${icon.name}' could not be resolved. Using default.")
                    defaultIcon(context)
                }
        }

        is ShortcutIcon.Resource -> {
            resolveDrawable(icon.name, context)
                ?: run {
                    Log.w(TAG, "Resource icon '${icon.name}' not found. Using default.")
                    defaultIcon(context)
                }
        }

        is ShortcutIcon.Bitmap -> {
            runCatching {
                val bmp = BitmapFactory.decodeByteArray(icon.data, 0, icon.data.size)
                IconCompat.createWithBitmap(bmp)
            }.getOrElse {
                Log.w(TAG, "Bitmap icon decode failed: ${it.message}. Using default.")
                defaultIcon(context)
            }
        }
    }

    private fun resolveDrawable(name: String, context: Context): IconCompat? {
        var resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId == 0) {
            resId = context.resources.getIdentifier(name, "drawable", "android")
        }
        if (resId == 0) return null
        return runCatching { IconCompat.createWithResource(context, resId) }.getOrNull()
    }

    private fun defaultIcon(context: Context): IconCompat =
        IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass)
}

/**
 * Best-effort SF Symbol → Android drawable name mapping.
 *
 * Falls back to the original name if no mapping is found, which allows
 * apps to bundle their own drawables using SF Symbol names directly.
 *
 * ## Registering custom mappings
 * Call [registerCustomMapping] at app startup to add your own SF Symbol → drawable pairs:
 * ```kotlin
 * MaterialSymbolMapper.registerCustomMapping("person.crop.circle", "ic_avatar")
 * MaterialSymbolMapper.registerCustomMapping("bolt",               "ic_flash")
 * ```
 *
 * Custom mappings take precedence over built-in ones.
 */
object MaterialSymbolMapper {
    private val builtIn = mapOf(
        // ── Editing ────────────────────────────────────────────────────────────
        "square.and.pencil"     to "ic_create",
        "pencil"                to "ic_create",

        // ── Communication ──────────────────────────────────────────────────────
        "message"               to "ic_message",
        "message.fill"          to "ic_message",
        "envelope"              to "ic_email",
        "envelope.fill"         to "ic_email",
        "phone"                 to "ic_phone",
        "phone.fill"            to "ic_phone",
        "paperplane"            to "ic_send",
        "paperplane.fill"       to "ic_send",
        "video"                 to "ic_videocam",
        "video.fill"            to "ic_videocam",
        "mic"                   to "ic_mic",
        "mic.fill"              to "ic_mic",

        // ── Navigation ─────────────────────────────────────────────────────────
        "house"                 to "ic_home",
        "house.fill"            to "ic_home",
        "arrow.right"           to "ic_arrow_forward",
        "arrow.left"            to "ic_arrow_back",
        "map"                   to "ic_map",
        "location"              to "ic_location_on",
        "location.fill"         to "ic_location_on",
        "globe"                 to "ic_language",

        // ── Search & Discovery ─────────────────────────────────────────────────
        "magnifyingglass"       to "ic_search",

        // ── People ─────────────────────────────────────────────────────────────
        "person"                to "ic_person",
        "person.fill"           to "ic_person",
        "person.2"              to "ic_group",
        "person.2.fill"         to "ic_group",

        // ── Media ──────────────────────────────────────────────────────────────
        "photo"                 to "ic_photo",
        "camera"                to "ic_camera_alt",
        "camera.fill"           to "ic_camera_alt",
        "music.note"            to "ic_music_note",
        "play"                  to "ic_play_arrow",
        "play.fill"             to "ic_play_arrow",
        "pause"                 to "ic_pause",
        "pause.fill"            to "ic_pause",
        "stop"                  to "ic_stop",
        "stop.fill"             to "ic_stop",

        // ── Actions ────────────────────────────────────────────────────────────
        "square.and.arrow.up"   to "ic_share",
        "plus"                  to "ic_add",
        "xmark"                 to "ic_close",
        "checkmark"             to "ic_check",
        "trash"                 to "ic_delete",
        "trash.fill"            to "ic_delete",

        // ── Content ────────────────────────────────────────────────────────────
        "star"                  to "ic_star",
        "star.fill"             to "ic_star",
        "heart"                 to "ic_favorite",
        "heart.fill"            to "ic_favorite",
        "bookmark"              to "ic_bookmark",
        "bookmark.fill"         to "ic_bookmark",
        "tag"                   to "ic_label",
        "tag.fill"              to "ic_label",
        "flag"                  to "ic_flag",
        "flag.fill"             to "ic_flag",
        "link"                  to "ic_link",
        "doc"                   to "ic_description",
        "doc.fill"              to "ic_description",
        "folder"                to "ic_folder",
        "folder.fill"           to "ic_folder",

        // ── Settings & System ──────────────────────────────────────────────────
        "gear"                  to "ic_settings",
        "bell"                  to "ic_notifications",
        "bell.fill"             to "ic_notifications",
        "lock"                  to "ic_lock",
        "lock.fill"             to "ic_lock",
        "key"                   to "ic_key",
        "wifi"                  to "ic_wifi",
        "battery.100"           to "ic_battery_full",
        "clock"                 to "ic_schedule",
        "calendar"              to "ic_calendar_today",
        "sun.max"               to "ic_wb_sunny",
        "sun.max.fill"          to "ic_wb_sunny",
        "moon"                  to "ic_nightlight",
        "moon.fill"             to "ic_nightlight",
        "lightbulb"             to "ic_lightbulb",
        "lightbulb.fill"        to "ic_lightbulb",

        // ── Commerce ──────────────────────────────────────────────────────────
        "cart"                  to "ic_shopping_cart",
        "cart.fill"             to "ic_shopping_cart",

        // ── Data & Charts ─────────────────────────────────────────────────────
        "square.stack"          to "ic_layers",
        "chart.bar"             to "ic_bar_chart",
        "chart.bar.fill"        to "ic_bar_chart",
        "qrcode"                to "ic_qr_code_scanner",

        // ── Help & Alerts ─────────────────────────────────────────────────────
        "questionmark"          to "ic_help",
        "exclamationmark"       to "ic_priority_high"
    )

    private val custom = mutableMapOf<String, String>()

    /**
     * Register a custom SF Symbol → Android drawable name mapping.
     * Custom mappings take precedence over built-in ones.
     *
     * Call at app startup (e.g. in `Application.onCreate()`):
     * ```kotlin
     * MaterialSymbolMapper.registerCustomMapping("person.crop.circle", "ic_avatar")
     * ```
     *
     * @param sfSymbol    The SF Symbol name (e.g. `"bolt"`, `"person.crop.circle"`).
     * @param drawableName The Android drawable resource name (without `R.drawable.`).
     */
    fun registerCustomMapping(sfSymbol: String, drawableName: String) {
        custom[sfSymbol] = drawableName
    }

    /**
     * Map [sfSymbol] to an Android drawable resource name.
     * Returns the custom mapping if registered, then the built-in mapping, then
     * falls back to [sfSymbol] itself (allowing apps to bundle drawables under the SF Symbol name).
     */
    fun map(sfSymbol: String): String = custom[sfSymbol] ?: builtIn[sfSymbol] ?: sfSymbol
}

