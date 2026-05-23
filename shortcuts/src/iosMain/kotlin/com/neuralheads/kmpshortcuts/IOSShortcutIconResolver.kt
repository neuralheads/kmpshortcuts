@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.neuralheads.kmpshortcuts

import com.neuralheads.kmpshortcuts.ShortcutIcon
import platform.UIKit.UIApplicationShortcutIcon

/**
 * Resolves a [ShortcutIcon] to a [UIApplicationShortcutIcon] for iOS quick actions.
 *
 * Fallback chain:
 * 1. SF Symbol by name (`UIApplicationShortcutIcon(systemImageName:)`)
 * 2. Asset catalog image (`UIApplicationShortcutIcon(templateImageName:)`)
 * 3. Default SF Symbol `"questionmark"` for unknown names
 * 4. `null` for [ShortcutIcon.None]
 */
internal object IOSShortcutIconResolver {

    fun resolve(icon: ShortcutIcon): UIApplicationShortcutIcon? = when (icon) {
        is ShortcutIcon.None -> null

        is ShortcutIcon.System ->
            UIApplicationShortcutIcon.iconWithSystemImageName(icon.name)
                ?: UIApplicationShortcutIcon.iconWithSystemImageName("questionmark")

        is ShortcutIcon.Resource ->
            UIApplicationShortcutIcon.iconWithTemplateImageName(icon.name)

        is ShortcutIcon.Bitmap ->
            // iOS does not support raw bitmap shortcuts icons;
            // the image must be in the asset catalog. Log and use default.
            UIApplicationShortcutIcon.iconWithSystemImageName("photo")
    }
}
