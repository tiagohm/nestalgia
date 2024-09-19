package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.ExcitingBoxingController
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys

class ExcitingBoxingSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = ExcitingBoxingController.Button.entries

    override fun defaultKey(button: ControllerButton) = when (button as ExcitingBoxingController.Button) {
        ExcitingBoxingController.Button.HIT_BODY -> KeyboardKeys.NUMBER_5
        ExcitingBoxingController.Button.HOOK_LEFT -> KeyboardKeys.NUMBER_7
        ExcitingBoxingController.Button.HOOK_RIGHT -> KeyboardKeys.NUMBER_9
        ExcitingBoxingController.Button.JAB_LEFT -> KeyboardKeys.NUMBER_1
        ExcitingBoxingController.Button.JAB_RIGHT -> KeyboardKeys.NUMBER_3
        ExcitingBoxingController.Button.MOVE_LEFT -> KeyboardKeys.NUMBER_4
        ExcitingBoxingController.Button.MOVE_RIGHT -> KeyboardKeys.NUMBER_6
        ExcitingBoxingController.Button.STRAIGHT -> KeyboardKeys.NUMBER_8
    }

    override fun onCreate() {
        title = "Exciting Boxing Punching Bag"

        super.onCreate()
    }
}
