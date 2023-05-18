package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Standard_controller

@Suppress("NOTHING_TO_INLINE")
open class StandardController(console: Console, port: Int) : ControlDevice(console, port) {

    private val keys = console.settings.getControllerKeys(port)

    private var microphoneEnabled = port == 1 && console.settings.consoleType == ConsoleType.FAMICOM
    // val turboSpeed = keys.turboSpeed
    // val turboFreq = (1 shl (4 - turboSpeed)) and 0xFF

    protected var stateBuffer = 0U

    protected inline val value: UByte
        get() {
            return ((if (isPressed(StandardControllerButton.A)) 0x01U else 0x00U) or
                (if (isPressed(StandardControllerButton.B)) 0x02U else 0x00U) or
                (if (isPressed(StandardControllerButton.SELECT)) 0x04U else 0x00U) or
                (if (isPressed(StandardControllerButton.START)) 0x08U else 0x00U) or
                (if (isPressed(StandardControllerButton.UP)) 0x10U else 0x00U) or
                (if (isPressed(StandardControllerButton.DOWN)) 0x20U else 0x00U) or
                (if (isPressed(StandardControllerButton.LEFT)) 0x40U else 0x00U) or
                (if (isPressed(StandardControllerButton.RIGHT)) 0x80U else 0x00U)).toUByte()
        }

    // inline val isTurboOn: Boolean
    //    get() = (console.frameCount % turboFreq) < (turboFreq / 2)

    private inline val isMicrophoneEnabled: Boolean
        get() = microphoneEnabled && console.frameCount % 3 == 0

    override fun setStateFromInput() {
        setPressedStateFromKeys(StandardControllerButton.A)
        setPressedStateFromKeys(StandardControllerButton.B)
        setPressedStateFromKeys(StandardControllerButton.START)
        setPressedStateFromKeys(StandardControllerButton.SELECT)
        setPressedStateFromKeys(StandardControllerButton.UP)
        setPressedStateFromKeys(StandardControllerButton.DOWN)
        setPressedStateFromKeys(StandardControllerButton.LEFT)
        setPressedStateFromKeys(StandardControllerButton.RIGHT)

        // if (isTurboOn) {
        //     setPressedStateFromKeys(Buttons.A)
        //     setPressedStateFromKeys(Buttons.B)
        // }

        if (isMicrophoneEnabled) {
            setPressedStateFromKeys(StandardControllerButton.MICROPHONE)
        }

        if (!console.settings.checkFlag(EmulationFlag.ALLOW_INVALID_INPUT)) {
            // If both U+D or L+R are pressed at the same time, act as if neither is pressed
            if (isPressed(StandardControllerButton.UP) && isPressed(StandardControllerButton.DOWN)) {
                setBit(StandardControllerButton.UP)
                setBit(StandardControllerButton.DOWN)
            }
            if (isPressed(StandardControllerButton.LEFT) && isPressed(StandardControllerButton.RIGHT)) {
                setBit(StandardControllerButton.LEFT)
                setBit(StandardControllerButton.RIGHT)
            }
        }
    }

    private inline fun setPressedStateFromKeys(button: Button) {
        setPressedState(button, keys.getKey(button))
    }

    override fun refreshStateBuffer() {
        val value = this.value

        stateBuffer = if (console.settings.consoleType == ConsoleType.NES &&
            console.settings.checkFlag(EmulationFlag.HAS_FOUR_SCORE)
        ) {
            if (port >= 2) {
                value.toUInt() shl 8
            } else {
                // Add some 0 bit padding to allow P3/P4 controller bits + signature bits
                (if (port == 0) 0xFF000000U else 0xFF000000U) or value.toUInt()
            }
        } else {
            0xFFFFFF00U or value.toUInt()
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        if (port >= 2 && console.isDualSystem) {
            // Ignore P3/P4 controllers for VS DualSystem - those are used by the slave CPU
            return 0U
        }

        var output: UByte = 0U

        if (addr.toUInt() == 0x4016U && (port and 0x01) == 0x00 ||
            addr.toUInt() == 0x4017U && (port and 0x01) == 0x01
        ) {
            strobeOnRead()

            output = (stateBuffer and 0x01U).toUByte()

            if (port >= 2 && console.settings.consoleType == ConsoleType.FAMICOM) {
                // Famicom outputs P3 & P4 on bit 1
                output = (output.toUInt() shl 1).toUByte()
            }

            stateBuffer = stateBuffer shr 1

            // All subsequent reads will return D=1 on an authentic controller but may return D=0 on third party controllers.
            stateBuffer = stateBuffer or 0x80000000U
        }

        if (addr.toUInt() == 0x4016U && isPressed(StandardControllerButton.MICROPHONE)) {
            output = output or 0x04U
        }

        return output
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        strobeOnWrite(value)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("stateBuffer", stateBuffer)
        s.write("microphoneEnabled", microphoneEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        stateBuffer = s.readUInt("stateBuffer") ?: 0U
        microphoneEnabled = s.readBoolean("microphoneEnabled") ?: false
    }
}
