package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Standard_controller

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
open class StandardController(console: Console, port: Int) :
    ControlDevice(console, port),
    Buttonable<StandardController.Buttons> {

    enum class Buttons(override val bit: Int) : Button {
        UP(0),
        DOWN(1),
        LEFT(2),
        RIGHT(3),
        START(4),
        SELECT(5),
        B(6),
        A(7),
        MICROPHONE(8),
    }

    private val keys = console.settings.getControllerKeys(port)

    val microphoneEnabled = port == 1 && console.settings.consoleType == ConsoleType.FAMICOM
    // val turboSpeed = keys.turboSpeed
    // val turboFreq = (1 shl (4 - turboSpeed)) and 0xFF

    protected var stateBuffer = 0U

    protected inline val value: UByte
        get() {
            return ((if (isPressed(Buttons.A)) 0x01U else 0x00U) or
                    (if (isPressed(Buttons.B)) 0x02U else 0x00U) or
                    (if (isPressed(Buttons.SELECT)) 0x04U else 0x00U) or
                    (if (isPressed(Buttons.START)) 0x08U else 0x00U) or
                    (if (isPressed(Buttons.UP)) 0x10U else 0x00U) or
                    (if (isPressed(Buttons.DOWN)) 0x20U else 0x00U) or
                    (if (isPressed(Buttons.LEFT)) 0x40U else 0x00U) or
                    (if (isPressed(Buttons.RIGHT)) 0x80U else 0x00U)).toUByte()
        }

    // inline val isTurboOn: Boolean
    //    get() = (console.frameCount % turboFreq) < (turboFreq / 2)

    inline val isMicrophoneEnabled: Boolean
        get() = microphoneEnabled && console.frameCount % 3 == 0

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

    override fun setStateFromInput() {
        setPressedStateFromKeys(Buttons.A)
        setPressedStateFromKeys(Buttons.B)
        setPressedStateFromKeys(Buttons.START)
        setPressedStateFromKeys(Buttons.SELECT)
        setPressedStateFromKeys(Buttons.UP)
        setPressedStateFromKeys(Buttons.DOWN)
        setPressedStateFromKeys(Buttons.LEFT)
        setPressedStateFromKeys(Buttons.RIGHT)

        // if (isTurboOn) {
        //     setPressedStateFromKeys(Buttons.A)
        //     setPressedStateFromKeys(Buttons.B)
        // }

        if (isMicrophoneEnabled) {
            setPressedStateFromKeys(Buttons.MICROPHONE)
        }

        if (!console.settings.checkFlag(EmulationFlag.ALLOW_INVALID_INPUT)) {
            // If both U+D or L+R are pressed at the same time, act as if neither is pressed
            if (isPressed(Buttons.UP) && isPressed(Buttons.DOWN)) {
                buttonDown(Buttons.UP)
                buttonDown(Buttons.DOWN)
            }
            if (isPressed(Buttons.LEFT) && isPressed(Buttons.RIGHT)) {
                buttonDown(Buttons.LEFT)
                buttonDown(Buttons.RIGHT)
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

        if (addr.toUInt() == 0x4016U && isPressed(Buttons.MICROPHONE)) {
            output = output or 0x04U
        }

        return output
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        strobeOnWrite(value)
    }
}