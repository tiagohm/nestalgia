package br.tiagohm.nestalgia.core

import java.io.Closeable

open class ControlManager(
    protected val console: Console,
    private val systemActionManager: ControlDevice,
    private val mapperControlDevice: ControlDevice?,
) : MemoryHandler, Resetable, Snapshotable, Closeable {

    private var lagging = false
    private val inputProviders = HashSet<InputProvider>()
    private val inputRecorders = HashSet<InputRecorder>()
    private val controlDevices = HashSet<ControlDevice>()

    @JvmField var pollCounter = 0
    @JvmField var lagCounter = 0

    fun registerInputProvider(inputProvider: InputProvider) {
        inputProviders.add(inputProvider)
    }

    fun unregisterInputProvider(inputProvider: InputProvider) {
        inputProviders.remove(inputProvider)
    }

    fun registerInputRecorder(inputRecorder: InputRecorder) {
        inputRecorders.add(inputRecorder)
    }

    fun unregisterInputRecorder(inputRecorder: InputRecorder) {
        inputRecorders.remove(inputRecorder)
    }

    val portState: List<ControlDeviceState>
        get() {
            val states = ArrayList<ControlDeviceState>(4)

            for (i in 0..3) {
                val device = controlDevice(i)
                states.add(device?.state ?: ControlDeviceState())
            }

            return states
        }

    override fun reset(softReset: Boolean) {
        lagCounter = 0
    }

    override fun close() {
        inputProviders.clear()
        inputRecorders.clear()
        controlDevices.clear()
    }

    fun updateControlDevices() {
        val settings = console.settings

        settings.needControllerUpdate()

        val hadKeyboard = hasKeyboard

        controlDevices.clear()

        registerControlDevice(systemActionManager)

        var fourScore = settings.flag(EmulationFlag.HAS_FOUR_SCORE)
        val consoleType = settings.consoleType
        var expansionPortDevice = settings.expansionPortDevice

        if (consoleType != ConsoleType.FAMICOM) {
            expansionPortDevice = ExpansionPortDevice.NONE
        } else if (expansionPortDevice != ExpansionPortDevice.FOUR_PLAYER_ADAPTER) {
            fourScore = false
        }

        repeat(if (fourScore) 4 else 2) {
            createControllerDevice(controllerType(it), it, console)
                ?.also(::registerControlDevice)
        }

        if (fourScore && consoleType == ConsoleType.NES) {
            // FourScore is only used to provide the signature for reads past the first 16 reads
            registerControlDevice(FourScore(console))
        }

        createExpansionDevice(expansionPortDevice, console)
            ?.also(::registerControlDevice)

        val hasKeyboard = this.hasKeyboard

        if (!hasKeyboard) {
            settings.isKeyboardMode = false
        } else if (!hadKeyboard) {
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
            ExpansionPortDevice.BATTLE_BOX -> BattleBox(console)
            else -> null
        }
    }

    fun updateInputState() {
        if (lagging) lagCounter++ else lagging = true

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

    protected open fun remapControllerButtons() {}

    val hasKeyboard
        get() = controlDevice(ControlDevice.EXP_DEVICE_PORT)?.keyboard ?: false

    protected open fun openBusMask(port: Int): Int {
        // In the NES and Famicom, the top three (or five) bits are not driven, and so retain the bits of the previous byte on the bus.
        // Usually this is the most significant byte of the address of the controller port - 0x40.
        // Paperboy relies on this behavior and requires that reads from the controller ports return exactly $40 or $41 as appropriate.
        when (console.settings.consoleType) {
            ConsoleType.FAMICOM -> {
                return if (console.settings.flag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)) {
                    if (port == 0) 0xF8 else 0xE0
                } else {
                    if (port == 0) 0xF8 else 0xE0
                }
            }
            else -> {
                return if (console.settings.flag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)) {
                    if (port == 0) 0xE4 else 0xE0
                } else {
                    0xE0
                }
            }
        }
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.READ, 0x4016, 0x4017)
        ranges.addHandler(MemoryOperation.WRITE, 0x4016)
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        lagging = false

        var value = console.memoryManager.openBus(openBusMask(addr - 0x4016))

        for (device in controlDevices) {
            value = value or device.read(addr, type)
        }

        return value
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        for (device in controlDevices) {
            device.write(addr, value, type)
        }
    }

    fun controlDevice(port: Int): ControlDevice? {
        return controlDevices.firstOrNull { it.port == port }
    }

    fun registerControlDevice(device: ControlDevice) {
        controlDevices.add(device)
    }

    protected open fun controllerType(port: Int): ControllerType {
        return console.settings.controllerType(port)
    }

    override fun saveState(s: Snapshot) {
        val region = console.region
        val expansionPortDevice = console.settings.expansionPortDevice
        val consoleType = console.settings.consoleType
        val hasFourScore = console.settings.flag(EmulationFlag.HAS_FOUR_SCORE)
        val useNes101Hvc101Behavior = console.settings.flag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)
        val asciiTurboFileSlot = console.settings.asciiTurboFileSlot
        val controllerTypes = Array(4) { console.settings.controllerType(it) }

        s.write("region", region)
        s.write("expansionPortDevice", expansionPortDevice)
        s.write("consoleType", consoleType)
        s.write("hasFourScore", hasFourScore)
        s.write("useNes101Hvc101Behavior", useNes101Hvc101Behavior)
        s.write("asciiTurboFileSlot", asciiTurboFileSlot)
        s.write("controllerTypes", controllerTypes)
        s.write("lagCounter", lagCounter)
        s.write("pollCounter", pollCounter)

        controlDevices.forEachIndexed { i, c -> s.write("controlDevice$i", c) }
    }

    override fun restoreState(s: Snapshot) {
        console.settings.region = s.readEnum("region", Region.AUTO)
        console.settings.expansionPortDevice = s.readEnum("expansionPortDevice", ExpansionPortDevice.NONE)
        console.settings.consoleType = s.readEnum("consoleType", ConsoleType.NES)
        val hasFourScore = s.readBoolean("hasFourScore")
        val useNes101Hvc101Behavior = s.readBoolean("useNes101Hvc101Behavior")
        console.settings.asciiTurboFileSlot = s.readInt("asciiTurboFileSlot")
        val controllerTypes = s.readArray<ControllerType>("controllerTypes")
        lagCounter = s.readInt("lagCounter")
        pollCounter = s.readInt("pollCounter")

        controllerTypes?.forEachIndexed { i, c -> console.settings.controllerType(i, c) }

        console.settings.flag(EmulationFlag.HAS_FOUR_SCORE, hasFourScore)
        console.settings.flag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR, useNes101Hvc101Behavior)

        controlDevices.forEachIndexed { i, c -> s.readSnapshotable("controlDevice$i", c) }
    }
}
