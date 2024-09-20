package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.BandaiMicrophone
import br.tiagohm.nestalgia.core.KeyMapping

open class BandaiMicrophoneSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = BandaiMicrophone.Button.entries
    override val defaultKeyMapping = KeyMapping()

    override fun onCreate() {
        title = "Bandai Microphone"

        super.onCreate()
    }
}
