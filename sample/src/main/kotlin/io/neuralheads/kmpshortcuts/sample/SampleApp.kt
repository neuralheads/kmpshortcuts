package io.neuralheads.kmpshortcuts.sample

import android.app.Application
import io.neuralheads.kmpshortcuts.KMPShortcuts
import io.neuralheads.kmpshortcuts.AndroidShortcutManager

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KMPShortcuts.initialize(AndroidShortcutManager(context = this))
    }
}
