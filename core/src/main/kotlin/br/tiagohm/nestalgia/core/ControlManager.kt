package br.tiagohm.nestalgia.core

import java.util.*

@ExperimentalUnsignedTypes
open class ControlManager(
    val console: Console,
    val systemActionManager: ControlDevice,
    val mapperControlDevice: ControlDevice?,
) : MemoryHandler,
    Resetable,
    Snapshotable,
    Disposable {

    private var isLagging = false
    private val inputProviders = ArrayList<InputProvider>()
    private val inputRecorders = ArrayList<InputRecorder>()
    private val controlDevices = ArrayList<ControlDevice>()

    var pollCounter = 0U
    var lagCounter = 0U

    @Synchronized
    fun registerInputProvider(inputProvider: InputProvider) {
        inputProviders.add(inputProvider)
    }

    @Synchronized
    fun unregisterInputProvider(inputProvider: InputProvider) {
        inputProviders.remove(inputProvider)
    }

    @Synchronized
    fun registerInputRecorder(inputRecorder: InputRecorder) {
        inputRecorders.add(inputRecorder)
    }

    @Synchronized
    fun unregisterInputRecorder(inputRecorder: InputRecorder) {
        inputRecorders.remove(inputRecorder)
    }

    val portState: List<ControlDeviceState>
        get() {
            val states = ArrayList<ControlDeviceState>(4)

            for (i in 0..3) {
                val device = getControlDevice(i)
                states.add(device?.state ?: ControlDeviceState())
            }

            return states
        }

    override fun reset(softReset: Boolean) {
        lagCounter = 0U
    }

    override fun dispose() {
        inputProviders.clear()
        inputRecorders.clear()
        controlDevices.clear()
    }

    open fun updateControlDevices() {
        val settings = console.settings

        settings.needControllerUpdate()

        val hadKeyboard = hasKeyboard

        controlDevices.clear()

        registerControlDevice(systemActionManager)

        var fourScore = settings.checkFlag(EmulationFlag.HAS_FOUR_SCORE)
        val consoleType = settings.consoleType
        var expansionDevice = settings.expansionDevice

        if (consoleType != ConsoleType.FAMICOM) {
            expansionDevice = ExpansionPortDevice.NONE
        } else if (expansionDevice != ExpansionPortDevice.FOUR_PLAYER_ADAPTER) {
            fourScore = false
        }

        for (i in 0 until if (fourScore) 4 else 2) {
            val device = createControllerDevice(getControllerType(i), i, console)

            if (device != null) {
                registerControlDevice(device)
            }
        }

        if (fourScore && consoleType == ConsoleType.NES) {
            // FourScore is only used to provide the signature for reads past the first 16 reads
            registerControlDevice(FourScore(console))
        }

        val expDevice = createExpansionDevice(expansionDevice, console)

        if (expDevice != null) {
            registerControlDevice(expDevice)
        }

        val hasKeyboard = this.hasKeyboard

        if (!hasKeyboard) {
            settings.isKeyboardMode = false
        } else if (!hadKeyboard && hasKeyboard) {
            settings.isKeyboardMode = true
        }

        if (mapperControlDevice != null) {
            registerControlDevice(mapperControlDevice)
        }

        /* TODO:
        if(std::dynamic_pointer_cast<FamilyBasicKeyboard>(expDevice)) {
		    // Automatically connect the data recorder if the keyboard is connected
		    RegisterControlDevice(shared_ptr<FamilyBasicDataRecorder>(new FamilyBasicDataRecorder(_console)));
	    }
         */
    }

    private fun createControllerDevice(type: ControllerType, port: Int, console: Console): ControlDevice? {
        return when (type) {
            ControllerType.STANDARD -> StandardController(console, port)
            ControllerType.ZAPPER -> Zapper(console, port)
            else -> null
        }
    }

    fun createExpansionDevice(type: ExpansionPortDevice, console: Console): ControlDevice? {
        return when (type) {
            ExpansionPortDevice.ZAPPER -> Zapper(console, ControlDevice.EXP_DEVICE_PORT)
            ExpansionPortDevice.ASCII_TURBO_FILE -> AsciiTurboFile(console)
            else -> null
        }
    }

    @Synchronized
    fun updateInputState() {
        if (isLagging) lagCounter++ else isLagging = true

        console.keyManager?.refreshKeyState()

        for (device in controlDevices) {
            device.clearState()

            for (provider in inputProviders) {
                provider.setInput(device)
            }

            device.setStateFromInput()
            device.onAfterSetState()
        }

        for (recorder in inputRecorders) {
            recorder.recordInput(controlDevices)
        }

        remapControllerButtons()

        pollCounter++
    }

    protected open fun remapControllerButtons() {
    }

    val hasKeyboard: Boolean
        get() = getControlDevice(ControlDevice.EXP_DEVICE_PORT)?.isKeyboard ?: false

    protected open fun getOpenBusMask(port: UByte): UByte {
        // In the NES and Famicom, the top three (or five) bits are not driven, and so retain the bits of the previous byte on the bus.
        // Usually this is the most significant byte of the address of the controller port - 0x40.
        // Paperboy relies on this behavior and requires that reads from the controller ports return exactly $40 or $41 as appropriate.
        when (console.settings.consoleType) {
            ConsoleType.FAMICOM -> {
                return if (console.settings.checkFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)) {
                    if (port.isZero) 0xF8U else 0xE0U
                } else {
                    if (port.isZero) 0xF8U else 0xE0U
                }
            }
            else -> {
                return if (console.settings.checkFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)) {
                    if (port.isZero) 0xE4U else 0xE0U
                } else {
                    0xE0U
                }
            }
        }
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.READ, 0x4016U, 0x4017U)
        ranges.addHandler(MemoryOperation.WRITE, 0x4016U)
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        isLagging = false
        var value = console.memoryManager.getOpenBus(getOpenBusMask((addr - 0x4016U).toUByte()))

        for (device in controlDevices) {
            value = value or device.read(addr, type)
        }

        return value
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        for (device in controlDevices) {
            device.write(addr, value, type)
        }
    }

    fun getControlDevice(port: Int): ControlDevice? {
        return controlDevices.firstOrNull { it.port == port }
    }

    fun registerControlDevice(device: ControlDevice) {
        controlDevices.add(device)
    }

    protected open fun getControllerType(port: Int): ControllerType {
        return console.settings.getControllerType(port)
    }

    override fun saveState(s: Snapshot) {
        val region = console.region
        val expansionDevice = console.settings.expansionDevice
        val consoleType = console.settings.consoleType
        val hasFourScore = console.settings.checkFlag(EmulationFlag.HAS_FOUR_SCORE)
        val useNes101Hvc101Behavior = console.settings.checkFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)
        val asciiTurboFileSlot = console.settings.asciiTurboFileSlot
        val controllerTypes = Array(4) { console.settings.getControllerType(it) }

        s.write("region", region)
        s.write("expansionDevice", expansionDevice)
        s.write("consoleType", consoleType)
        s.write("hasFourScore", hasFourScore)
        s.write("useNes101Hvc101Behavior", useNes101Hvc101Behavior)
        s.write("asciiTurboFileSlot", asciiTurboFileSlot)
        s.write("controllerTypes", controllerTypes)
        s.write("lagCounter", lagCounter)
        s.write("pollCounter", pollCounter)

        for (i in controlDevices.indices) {
            s.write("controlDevice$i", controlDevices[i])
        }
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        console.settings.region = s.readEnum("region") ?: Region.AUTO
        console.settings.expansionDevice = s.readEnum("expansionDevice") ?: ExpansionPortDevice.NONE
        console.settings.consoleType = s.readEnum("consoleType") ?: ConsoleType.NES
        val hasFourScore = s.readBoolean("hasFourScore") ?: false
        val useNes101Hvc101Behavior = s.readBoolean("useNes101Hvc101Behavior") ?: false
        console.settings.asciiTurboFileSlot = s.readInt("asciiTurboFileSlot") ?: 0
        val controllerTypes = s.readEnumArray<ControllerType>("controllerTypes")
        lagCounter = s.readUInt("lagCounter") ?: 0U
        pollCounter = s.readUInt("pollCounter") ?: 0U

        controllerTypes?.forEachIndexed { i, c -> console.settings.setControllerType(i, c) }

        if (hasFourScore) console.settings.setFlag(EmulationFlag.HAS_FOUR_SCORE)
        else console.settings.clearFlag(EmulationFlag.HAS_FOUR_SCORE)

        if (useNes101Hvc101Behavior) console.settings.setFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)
        else console.settings.clearFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)

        for (i in controlDevices.indices) {
            s.readSnapshot("controlDevice$i")?.let { controlDevices[i].restoreState(it) }
        }
    }
}
