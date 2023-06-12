package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*

open class SystemActionManager(console: Console) : ControlDevice(console, NONE, CONSOLE_INPUT_PORT) {

    private var needReset = false
    private var needPowerCycle = false

    val isResetPending
        get() = needReset || needPowerCycle

    override fun reset(softReset: Boolean) {
        if (!needReset) {
            needReset = true
        }
    }

    override fun onAfterSetState() {
        if (needReset) {
            setBit(SystemActionButton.RESET)
        }

        if (needPowerCycle) {
            setBit(SystemActionButton.POWER)
        }
    }

    fun processSystemActions() {
        if (isPressed(SystemActionButton.RESET)) {
            needReset = false
            console.resetComponents(true)
            console.controlManager.updateInputState()
        }

        if (isPressed(SystemActionButton.POWER)) {
            console.powerCycle()
        }
    }

    fun softReset(): Boolean {
        return if (!needReset) {
            needReset = true
            true
        } else {
            false
        }
    }

    fun powerCycle(): Boolean {
        return if (!needPowerCycle) {
            needPowerCycle = true
            true
        } else {
            false
        }
    }
}
