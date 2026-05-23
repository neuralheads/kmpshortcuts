package com.neuralheads.kmpshortcuts

/**
 * Describes the icon for an app shortcut.
 *
 * Use the factory functions for platform-appropriate icons:
 * ```kotlin
 * // SF Symbol on iOS, auto-maps on Android:
 * ShortcutIcon.System("square.and.pencil")
 *
 * // Drawable resource name (Android) / asset catalog name (iOS):
 * ShortcutIcon.Resource("ic_new_post")
 *
 * // PNG bytes — platform will decode appropriately:
 * ShortcutIcon.Bitmap(byteArray)
 * ```
 */
sealed class ShortcutIcon {

    /** No icon — platform will show a default placeholder. */
    data object None : ShortcutIcon()

    /**
     * System-provided icon.
     * - **Android**: maps to a Material icon via name (see [MaterialSymbolMapper]).
     *   Falls back to `android.R.drawable.ic_menu_compass` if unknown.
     * - **iOS**: SF Symbol name (e.g. `"square.and.pencil"`, `"message"`).
     *   Falls back to `"questionmark"` if unknown.
     *
     * @param name SF Symbol / Material symbol name.
     */
    data class System(val name: String) : ShortcutIcon()

    /**
     * App-bundled icon by resource name.
     * - **Android**: drawable resource name looked up via `getIdentifier()`.
     * - **iOS**: asset catalog image name.
     *
     * @param name Resource / asset name (without extension).
     */
    data class Resource(val name: String) : ShortcutIcon()

    /**
     * Raw PNG/JPEG bytes. Decoded to a platform bitmap.
     *
     * @param data Raw image bytes.
     */
    data class Bitmap(val data: ByteArray) : ShortcutIcon() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Bitmap) return false
            return data.contentEquals(other.data)
        }
        override fun hashCode(): Int = data.contentHashCode()
    }
}
