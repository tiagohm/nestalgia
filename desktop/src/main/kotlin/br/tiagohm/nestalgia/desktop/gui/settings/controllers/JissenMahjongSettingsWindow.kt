package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.JissenMahjong
import br.tiagohm.nestalgia.core.KeyMapping
import javafx.stage.Screen

class JissenMahjongSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = JissenMahjong.Button.entries
    override val defaultKeyMapping = JissenMahjong.defaultKeyMapping()

    override fun onCreate() {
        title = "Jissen Mahjong"

        super.onCreate()

        window.maxHeight = Screen.getPrimary().visualBounds.height * 0.9
    }
}
