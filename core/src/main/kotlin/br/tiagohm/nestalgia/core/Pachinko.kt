package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/Coconuts_Japan_Pachinko_Controller

class Pachinko(console: Console, keyMapping: KeyMapping) : StandardController(console, ControllerType.PACHINKO, EXP_DEVICE_PORT, keyMapping) {

    @Volatile private var analogData = 0

    enum class Button(override val bit: Int) : ControllerButton, HasCustomKey {
        PRESS(8),
        RELEASE(9);

        override val keyIndex = 30 + ordinal
    }

    private val keys = Button.entries.map(keyMapping::customKey).toTypedArray()

    override fun setStateFromInput() {
        super.setStateFromInput()
        Button.entries.forEach { setPressedState(it, keys[it.ordinal]) }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var output = 0

        if (addr == 0x4016) {
            strobeOnRead()
            output = (stateBuffer and 0x01) shl 1
            stateBuffer = stateBuffer shr 1
        }

        return output
    }

    override fun refreshStateBuffer() {
        if (analogData < 0x63 && isPressed(Button.PRESS)) {
            analogData++
        } else if (analogData > 0 && isPressed(Button.RELEASE)) {
            analogData--
        }

        val data = (analogData and 0x01 shl 7) or
            (analogData and 0x02 shl 5) or
            (analogData and 0x04 shl 3) or
            (analogData and 0x08 shl 1) or
            (analogData and 0x10 shr 1) or
            (analogData and 0x20 shr 3) or
            (analogData and 0x40 shr 5) or
            (analogData and 0x80 shr 7)

        super.refreshStateBuffer()

        stateBuffer = (stateBuffer and 0xFF) or (data.inv() and 0xFF shl 8)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("analogData", analogData)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        analogData = s.readInt("analogData")
    }

    companion object : HasDefaultKeyMapping {

        override fun defaultKeyMapping() = StandardController.defaultKeyMapping().also(::populateWithDefault)

        override fun populateWithDefault(keyMapping: KeyMapping) {
            keyMapping.customKey(Button.PRESS, KeyboardKeys.F)
            keyMapping.customKey(Button.RELEASE, KeyboardKeys.R)
        }
    }
}
