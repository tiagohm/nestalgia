package br.tiagohm.nestalgia.core

abstract class ControlDevice(
    @JvmField protected val console: Console,
    @JvmField val port: Int,
) : Memory, Resetable, Snapshotable {

    @JvmField protected var strobe = false

    protected val expansionPortDevice = port == EXP_DEVICE_PORT

    @JvmField internal val state = ControlDeviceState()

    open val keyboard = false

    override fun reset(softReset: Boolean) {}

    protected fun isCurrentPort(addr: Int): Boolean {
        return port == (addr - 0x4016)
    }

    protected open fun refreshStateBuffer() {}

    protected fun strobeOnRead() {
        if (strobe) refreshStateBuffer()
    }

    protected fun strobeOnWrite(value: Int) {
        val prevStrobe = strobe

        strobe = value.bit0

        if (prevStrobe && !strobe) {
            refreshStateBuffer()
        }
    }

    fun clearState() {
        state.clear()
    }

    protected fun isPressed(button: ControllerButton): Boolean {
        val bit = button.bit
        val bitMask = 1 shl (bit % 8)
        return (state.state[bit / 8] and bitMask) != 0
    }

    protected fun isPressed(bit: Int): Boolean {
        val bitMask = 1 shl (bit % 8)
        return (state.state[bit / 8] and bitMask) != 0
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun setBit(button: ControllerButton) {
        setBit(button.bit)
    }

    @PublishedApi
    internal fun setBit(bit: Int) {
        val bitMask = 1 shl (bit % 8)
        val byteIndex = bit / 8
        val value = state.state[byteIndex]
        state.state[byteIndex] = value or bitMask
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun clearBit(button: ControllerButton) {
        clearBit(button.bit)
    }

    @PublishedApi
    internal fun clearBit(bit: Int) {
        val bitMask = 1 shl (bit % 8)
        val byteIndex = bit / 8
        val value = state.state[byteIndex]
        state.state[byteIndex] = value and bitMask.inv()
    }

    protected fun setPressedState(button: ControllerButton, keyCode: Int) {
        if (keyboard && keyCode < 0x200 && !console.settings.isKeyboardMode) {
            // Prevent keyboard device input when keyboard mode is off
            return
        }

        if (console.keyManager != null &&
            console.settings.inputEnabled &&
            (!console.settings.isKeyboardMode || keyCode >= 0x200 || keyboard) &&
            console.keyManager!!.isKeyPressed(keyCode)
        ) {
            setBit(button)
        }
    }

    open fun setStateFromInput() {}

    open fun onAfterSetState() {}

    override fun saveState(s: Snapshot) {
        s.write("strobe", strobe)
        s.write("state", state.state)
    }

    override fun restoreState(s: Snapshot) {
        strobe = s.readBoolean("strobe")
        s.readIntArray("state", state.state)
    }

    companion object {

        const val EXP_DEVICE_PORT = 4
        const val CONSOLE_INPUT_PORT = 5
        const val MAPPER_INPUT_PORT = 6
        const val EXP_DEVICE_PORT_2 = 7
        const val PORT_COUNT = 8
    }
}
