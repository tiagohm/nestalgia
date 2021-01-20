package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
abstract class ControlDevice(
    val console: Console,
    val port: Int,
) : Memory,
    Resetable,
    Snapshotable {

    protected var strobe = false

    val isExpansionDevice = port == EXP_DEVICE_PORT

    val state = ControlDeviceState()

    open val isKeyboard = false

    override fun reset(softReset: Boolean) {
    }

    protected inline fun isCurrentPort(addr: UShort): Boolean {
        return port == (addr - 0x4016U).toInt()
    }

    protected open fun refreshStateBuffer() {
    }

    protected inline fun strobeOnRead() {
        if (strobe) refreshStateBuffer()
    }

    protected inline fun strobeOnWrite(value: UByte) {
        val prevStrobe = strobe
        strobe = value.bit0

        if (prevStrobe && !strobe) {
            refreshStateBuffer()
        }
    }

    inline fun clearState() {
        state.clear()
    }

    protected inline fun isPressed(button: Button): Boolean {
        return isPressed(button.bit)
    }

    protected inline fun isPressed(bit: Int): Boolean {
        val bitMask = 1 shl (bit % 8)
        return (state.state[bit / 8].toInt() and bitMask) != 0
    }

    fun setBit(button: Button) {
        setBit(button.bit)
    }

    protected inline fun setBit(bit: Int) {
        val bitMask = 1 shl (bit % 8)
        val byteIndex = bit / 8
        val value = state.state[byteIndex].toInt()
        state.state[byteIndex] = (value or bitMask).toUByte()
    }

    fun clearBit(button: Button) {
        clearBit(button.bit)
    }

    protected inline fun clearBit(bit: Int) {
        val bitMask = 1 shl (bit % 8)
        val byteIndex = bit / 8
        val value = state.state[byteIndex].toInt()
        state.state[byteIndex] = (value and bitMask.inv()).toUByte()
    }

    fun invertBit(button: Button) {
        invertBit(button.bit)
    }

    protected inline fun invertBit(bit: Int) {
        if (isPressed(bit)) clearBit(bit) else setBit(bit)
    }

    protected fun setPressedState(button: Button, keyCode: Int) {
        if (isKeyboard && keyCode < 0x200 && !console.settings.isKeyboardMode) {
            // Prevent keyboard device input when keyboard mode is off
            return
        }

        if (console.keyManager != null &&
            console.settings.isInputEnabled &&
            (!console.settings.isKeyboardMode || keyCode >= 0x200 || isKeyboard) &&
            console.keyManager!!.isKeyPressed(keyCode)
        ) {
            setBit(button)
        }
    }

    open fun setStateFromInput() {
    }

    open fun onAfterSetState() {
    }

    override fun saveState(s: Snapshot) {
        s.write("strobe", strobe)
        s.write("state", state.state)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        strobe = s.readBoolean("strobe") ?: false
        s.readUByteArray("state")?.copyInto(state.state)
    }

    companion object {
        const val EXP_DEVICE_PORT = 4
        const val CONSOLE_INPUT_PORT = 5
        const val MAPPER_INPUT_PORT = 6
        const val EXP_DEVICE_PORT_2 = 7
        const val PORT_COUNT = 8
    }
}