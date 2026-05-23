package com.neuralheads.kmpshortcuts.android

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import com.neuralheads.kmpshortcuts.ShortcutIcon

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
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId == 0) return null
        return runCatching { IconCompat.createWithResource(context, resId) }.getOrNull()
    }

    private fun defaultIcon(context: Context): IconCompat =
        IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass)
}

/**
 * Best-effort SF Symbol → Android drawable name mapping.
 * Falls back to the original name if no mapping is found.
 */
internal object MaterialSymbolMapper {
    private val map = mapOf(
        "square.and.pencil"   to "ic_create",
        "message"             to "ic_message",
        "message.fill"        to "ic_message",
        "house"               to "ic_home",
        "house.fill"          to "ic_home",
        "person"              to "ic_person",
        "person.fill"         to "ic_person",
        "magnifyingglass"     to "ic_search",
        "star"                to "ic_star",
        "star.fill"           to "ic_star",
        "heart"               to "ic_favorite",
        "heart.fill"          to "ic_favorite",
        "gear"                to "ic_settings",
        "bell"                to "ic_notifications",
        "bell.fill"           to "ic_notifications",
        "camera"              to "ic_camera_alt",
        "camera.fill"         to "ic_camera_alt",
        "photo"               to "ic_photo",
        "trash"               to "ic_delete",
        "trash.fill"          to "ic_delete",
        "plus"                to "ic_add",
        "xmark"               to "ic_close",
        "checkmark"           to "ic_check",
        "arrow.right"         to "ic_arrow_forward",
        "arrow.left"          to "ic_arrow_back",
        "bookmark"            to "ic_bookmark",
        "bookmark.fill"       to "ic_bookmark",
        "link"                to "ic_link",
        "globe"               to "ic_language",
        "envelope"            to "ic_email",
        "envelope.fill"       to "ic_email",
        "phone"               to "ic_phone",
        "phone.fill"          to "ic_phone",
        "map"                 to "ic_map",
        "location"            to "ic_location_on",
        "location.fill"       to "ic_location_on",
        "cart"                to "ic_shopping_cart",
        "cart.fill"           to "ic_shopping_cart",
        "questionmark"        to "ic_help",
        "exclamationmark"     to "ic_priority_high"
    )

    fun map(sfSymbol: String): String = map[sfSymbol] ?: sfSymbol
}
