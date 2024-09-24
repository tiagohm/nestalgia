package br.tiagohm.nestalgia.core

import org.slf4j.LoggerFactory

@Suppress("NOTHING_TO_INLINE")
abstract class ControlDevice(
    @JvmField protected val console: Console,
    @JvmField val type: ControllerType,
    @JvmField val port: Int,
) : Memory, Resetable, Snapshotable {

    @JvmField @Volatile protected var strobe = false

    inline val isExpansionDevice
        get() = port == EXP_DEVICE_PORT

    @JvmField @PublishedApi internal val state = ControlDeviceState()

    override fun reset(softReset: Boolean) = Unit

    protected inline fun isCurrentPort(addr: Int): Boolean {
        return port == (addr - 0x4016)
    }

    protected open fun refreshStateBuffer() = Unit

    protected inline fun strobeOnRead() {
        if (strobe) refreshStateBuffer()
    }

    protected fun strobeOnWrite(value: Int) {
        val prevStrobe = strobe

        strobe = value.bit0

        if (prevStrobe && !strobe) {
            refreshStateBuffer()
        }
    }

    inline fun clearState() {
        state.clear()
    }

    protected inline fun isPressed(button: ControllerButton): Boolean {
        return isPressed(button.bit)
    }

    protected fun isPressed(bit: Int): Boolean {
        val bitMask = 1 shl (bit % 8)
        return (state[bit / 8] and bitMask) != 0
    }

    inline fun setBit(button: ControllerButton) {
        setBit(button.bit)
    }

    @PublishedApi
    internal fun setBit(bit: Int) {
        val bitMask = 1 shl (bit % 8)
        val byteIndex = bit / 8
        val value = state[byteIndex]
        state[byteIndex] = value or bitMask
    }

    inline fun clearBit(button: ControllerButton) {
        clearBit(button.bit)
    }

    @PublishedApi
    internal fun clearBit(bit: Int) {
        val bitMask = 1 shl (bit % 8)
        val byteIndex = bit / 8
        val value = state[byteIndex]
        state[byteIndex] = value and bitMask.inv()
    }

    protected fun setPressedState(button: ControllerButton, key: Key) {
        if (console.settings.isInputEnabled && console.keyManager.isKeyPressed(key)) {
            setBit(button)
        }
    }

    open fun setStateFromInput() = Unit

    open fun onAfterSetState() = Unit

    open fun hasControllerType(type: ControllerType): Boolean {
        return this.type == type
    }

    override fun saveState(s: Snapshot) {
        s.write("strobe", strobe)
        s.write("state", state)
        s.write("type", type)
        s.write("port", port)
    }

    override fun restoreState(s: Snapshot) {
        if (s.readEnum("type", type) == type && s.readInt("port", port) == port) {
            strobe = s.readBoolean("strobe")
            s.readSnapshotable("state", state)
        } else {
            LOG.warn("unable to restore state for control device. type={}, port={}", type, port)
        }
    }

    companion object {

        const val EXP_DEVICE_PORT = 4
        const val CONSOLE_INPUT_PORT = 5
        const val MAPPER_INPUT_PORT = 6
        const val EXP_DEVICE_PORT_2 = 7
        const val PORT_COUNT = 8

        private val LOG = LoggerFactory.getLogger(ControlDevice::class.java)

        internal fun swapButtons(
            device1: ControlDevice, button1: ControllerButton,
            device2: ControlDevice, button2: ControllerButton,
        ) {
            val pressed1 = device1.isPressed(button1)
            val pressed2 = device2.isPressed(button2)

            device1.clearBit(button1)
            device2.clearBit(button2)

            if (pressed1) {
                device2.setBit(button2)
            }
            if (pressed2) {
                device1.setBit(button1)
            }
        }
    }
}
