package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ConsoleType.*
import br.tiagohm.nestalgia.core.ControlDevice.Companion.PORT_COUNT
import br.tiagohm.nestalgia.core.ControllerType.*
import java.io.Closeable

open class ControlManager(protected val console: Console) : MemoryHandler, Resetable, Snapshotable, Closeable {

    private val inputProviders = HashSet<InputProvider>()
    private val inputRecorders = HashSet<InputRecorder>()
    private val systemDevices = HashSet<ControlDevice>()
    private val controlDevices = HashSet<ControlDevice>()

    @JvmField internal var pollCounter = 0
    @JvmField internal var lagCounter = 0
    @JvmField internal var wasInputRead = false

    init {
        systemDevices.add(console.systemActionManager)
    }

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

    fun addSystemControlDevice(device: ControlDevice) {
        controlDevices.clear()
        systemDevices.add(device)
        updateControlDevices(true)
    }

    override fun reset(softReset: Boolean) {
        lagCounter = 0
    }

    override fun close() {
        inputProviders.clear()
        inputRecorders.clear()
        controlDevices.clear()
    }

    protected fun saveBattery() {
        for (device in controlDevices) {
            if (device is Battery) {
                device.saveBattery()
            }
        }
    }

    protected fun clearDevices() {
        controlDevices.clear()

        systemDevices.forEach(::registerControlDevice)
    }

    fun updateControlDevices(force: Boolean = false) {
        val settings = console.settings

        if (!force && !settings.needControllerUpdate()) {
            return
        }

        val hadKeyboard = hasKeyboard

        saveBattery()

        clearDevices()

        repeat(2) {
            createControllerDevice(controllerType(it), it)
                ?.also(::registerControlDevice)
        }

        val expansionDevice = createControllerDevice(settings.expansionPortDevice, ControlDevice.EXP_DEVICE_PORT)

        if (expansionDevice != null) {
            registerControlDevice(expansionDevice)

//            if (std::dynamic_pointer_cast<FamilyBasicKeyboard>(expDevice)) {
//                //Automatically connect the data recorder if the family basic keyboard is connected
//                RegisterControlDevice(shared_ptr<FamilyBasicDataRecorder>(new FamilyBasicDataRecorder (_emu)));
//            }
        }

        val hasKeyboard = this.hasKeyboard

        if (!hasKeyboard) {
            settings.isKeyboardMode = false
        } else if (!hadKeyboard) {
            settings.isKeyboardMode = true
        }
    }

    private fun createControllerDevice(type: ControllerType, port: Int): ControlDevice? {
        return when (type) {
            NES_CONTROLLER,
            FAMICOM_CONTROLLER,
            FAMICOM_CONTROLLER_P2 -> StandardController(console, type, port)
            NES_ZAPPER -> Zapper(console, type, port)
            FAMICOM_ZAPPER -> Zapper(console, type, port)
            ASCII_TURBO_FILE -> AsciiTurboFile(console)
            BATTLE_BOX -> BattleBox(console)
            else -> null
        }
    }

    fun processEndOfFrame() {
        if (!wasInputRead) {
            lagCounter++
        }

        wasInputRead = false
    }

    fun updateInputState() {
        console.keyManager?.refreshKeyState()

        for (device in controlDevices) {
            device.clearState()
            device.setStateFromInput()

            for (provider in inputProviders) {
                provider.setInput(device)
            }

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

    open fun openBusMask(port: Int): Int {
        // In the NES and Famicom, the top three (or five) bits are not driven,
        // and so retain the bits of the previous byte on the bus.
        // Usually this is the most significant byte of the address of the
        // controller port - 0x40.
        // Paperboy relies on this behavior and requires that reads from the
        // controller ports return exactly $40 or $41 as appropriate.
        return when (console.settings.consoleType) {
            NES_101 -> if (port == 0) 0xE4 else 0xE0
            HVC_001,
            HVC_101 -> if (port == 0) 0xF8 else 0xE0
            else -> 0xE0
        }
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.READ, 0x4016, 0x4017)
        ranges.addHandler(MemoryOperation.WRITE, 0x4016)
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        // Used for lag counter - any frame where the input is read does not count as lag.
        wasInputRead = true

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
        val asciiTurboFileSlot = console.settings.asciiTurboFileSlot
        val controllerTypes = Array(PORT_COUNT) { console.settings.controllerType(it) }

        s.write("region", region)
        s.write("expansionPortDevice", expansionPortDevice)
        s.write("consoleType", consoleType)
        s.write("asciiTurboFileSlot", asciiTurboFileSlot)
        s.write("controllerTypes", controllerTypes)
        s.write("lagCounter", lagCounter)
        s.write("pollCounter", pollCounter)

        controlDevices.forEachIndexed { i, c -> s.write("controlDevice$i", c) }
    }

    override fun restoreState(s: Snapshot) {
        console.settings.region = s.readEnum("region", Region.AUTO)
        console.settings.expansionPortDevice = s.readEnum("expansionPortDevice", NONE)
        console.settings.consoleType = s.readEnum("consoleType", NES_001)
        console.settings.asciiTurboFileSlot = s.readInt("asciiTurboFileSlot")
        val controllerTypes = s.readArray<ControllerType>("controllerTypes")
        lagCounter = s.readInt("lagCounter")
        pollCounter = s.readInt("pollCounter")

        controllerTypes?.forEachIndexed { i, c -> console.settings.controllerType(i, c) }
        controlDevices.forEachIndexed { i, c -> s.readSnapshotable("controlDevice$i", c) }
    }
}
