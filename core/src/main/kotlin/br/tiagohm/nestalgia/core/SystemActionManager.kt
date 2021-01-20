package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
open class SystemActionManager(console: Console) : ControlDevice(console, CONSOLE_INPUT_PORT) {

    private var isNeedReset: Boolean = false
    private var isNeedPowerCycle: Boolean = false

    override fun reset(softReset: Boolean) {
        if (!isNeedReset) {
            isNeedReset = true
        }
    }

    override fun onAfterSetState() {
        if (isNeedReset) {
            setBit(SystemActionButton.RESET)
        }

        if (isNeedPowerCycle) {
            setBit(SystemActionButton.POWER)
        }
    }

    fun processSystemActions() {
        if (isPressed(SystemActionButton.RESET)) {
            isNeedReset = false
            console.resetComponents(true)
            console.controlManager.updateInputState()
        }

        if (isPressed(SystemActionButton.POWER)) {
            console.powerCycle()
        }
    }

    fun reset(): Boolean {
        return if (!isNeedReset) {
            isNeedReset = true
            true
        } else {
            false
        }
    }

    fun powerCycle(): Boolean {
        return if (!isNeedPowerCycle) {
            isNeedPowerCycle = true
            true
        } else {
            false
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte = 0U

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
    }
}
