package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
open class SystemActionManager(console: Console) :
    ControlDevice(console, CONSOLE_INPUT_PORT),
    Buttonable<SystemActionManager.Buttons> {
    private var isNeedReset: Boolean = false
    private var isNeedPowerCycle: Boolean = false

    enum class Buttons(override val bit: Int) : Button {
        RESET(0),
        POWER(1),
    }

    override fun reset(softReset: Boolean) {
        if (!isNeedReset) {
            isNeedReset = true
        }
    }

    override fun onAfterSetState() {
        if (isNeedReset) {
            buttonDown(Buttons.RESET)
        }
        if (isNeedPowerCycle) {
            buttonDown(Buttons.POWER)
        }
    }

    @Synchronized
    override fun buttonDown(button: Buttons) {
        setBit(button.bit)
    }

    @Synchronized
    override fun buttonUp(button: Buttons) {
        clearBit(button.bit)
    }

    override fun isPressed(button: Buttons): Boolean {
        return isPressed(button.bit)
    }

    fun processSystemActions() {
        if (isPressed(Buttons.RESET)) {
            isNeedReset = false
            console.resetComponents(true)
            console.controlManager.updateInputState()
        }
        if (isPressed(Buttons.POWER)) {
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
