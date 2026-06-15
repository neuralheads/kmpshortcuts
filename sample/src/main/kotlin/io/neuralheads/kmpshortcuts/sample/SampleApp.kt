package io.neuralheads.kmpshortcuts.sample

import android.app.Application
import io.neuralheads.kmpshortcuts.KMPShortcuts
import io.neuralheads.kmpshortcuts.AndroidShortcutManager
import io.neuralheads.kmpshortcuts.AndroidShortcutBadge

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KMPShortcuts.initialize(
            manager = AndroidShortcutManager(context = this),
            badge = AndroidShortcutBadge(context = this)
        )
    }
}
