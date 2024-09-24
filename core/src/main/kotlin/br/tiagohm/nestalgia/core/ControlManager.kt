package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ConsoleType.*
import br.tiagohm.nestalgia.core.ControlDevice.Companion.EXP_DEVICE_PORT
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE
import org.slf4j.LoggerFactory

open class ControlManager(protected val console: Console) : MemoryHandler, Resetable, Initializable, Snapshotable, AutoCloseable {

    private val inputProviders = HashSet<InputProvider>(1)
    private val inputRecorders = HashSet<InputRecorder>(1)
    private val systemDevices = HashSet<ControlDevice>(1)
    private val controlDevices = HashSet<ControlDevice>(2)
    private val controlManagerListeners = HashSet<ControlManagerListener>(1)

    @JvmField internal var pollCounter = 0
    @JvmField internal var lagCounter = 0
    @JvmField internal var wasInputRead = false

    override fun initialize() {
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

    fun registerControlManagerListener(listener: ControlManagerListener) {
        controlManagerListeners.add(listener)
    }

    fun unregisterControlManagerListener(listener: ControlManagerListener) {
        controlManagerListeners.remove(listener)
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

        saveBattery()

        clearDevices()

        createControllerDevice(settings.port1, 0)?.also(::registerControlDevice)
        createControllerDevice(settings.port2, 1)?.also(::registerControlDevice)

        val expansionDevice = createControllerDevice(settings.expansionPort, EXP_DEVICE_PORT)

        if (expansionDevice != null) {
            registerControlDevice(expansionDevice)

            // if (std::dynamic_pointer_cast<FamilyBasicKeyboard>(expDevice)) {
            //     // TODO: Automatically connect the data recorder if the family basic keyboard is connected
            //     RegisterControlDevice(shared_ptr<FamilyBasicDataRecorder>(new FamilyBasicDataRecorder (_emu)));
            // }
        }
    }

    private fun createControllerDevice(settings: ControllerSettings, port: Int): ControlDevice? {
        val type = settings.type
        val keyMapping = settings.keyMapping

        settings.populateKeyMappingWithDefault()

        val device = when (type) {
            NES_CONTROLLER,
            FAMICOM_CONTROLLER,
            FAMICOM_CONTROLLER_P2 -> StandardController(console, type, port, keyMapping)
            NES_ZAPPER -> Zapper(console, type, port, keyMapping)
            FAMICOM_ZAPPER -> Zapper(console, type, EXP_DEVICE_PORT, keyMapping)
            ASCII_TURBO_FILE -> AsciiTurboFile(console)
            BATTLE_BOX -> BattleBox(console)
            FOUR_SCORE -> FourScore(console, type, 0, *console.settings.subPort1)
            TWO_PLAYER_ADAPTER -> TwoPlayerAdapter(console, type, *console.settings.expansionSubPort)
            FOUR_PLAYER_ADAPTER -> FourScore(console, type, EXP_DEVICE_PORT, *console.settings.expansionSubPort)
            NES_ARKANOID_CONTROLLER -> ArkanoidController(console, type, port, keyMapping)
            FAMICOM_ARKANOID_CONTROLLER -> ArkanoidController(console, type, EXP_DEVICE_PORT, keyMapping)
            POWER_PAD_SIDE_A,
            POWER_PAD_SIDE_B -> PowerPad(console, type, port, keyMapping)
            EXCITING_BOXING -> ExcitingBoxingController(console, keyMapping)
            BANDAI_HYPER_SHOT -> BandaiHyperShot(console, keyMapping)
            FAMILY_TRAINER_MAT_SIDE_A,
            FAMILY_TRAINER_MAT_SIDE_B -> FamilyTrainerMat(console, type, keyMapping)
            KONAMI_HYPER_SHOT -> KonamiHyperShot(console, keyMapping)
            HORI_TRACK -> HoriTrack(console, keyMapping)
            PACHINKO -> Pachinko(console, keyMapping)
            PARTY_TAP -> PartyTap(console, keyMapping)
            JISSEN_MAHJONG -> JissenMahjong(console, keyMapping)
            SUBOR_MOUSE -> SuborMouse(console, port, keyMapping)
            SUBOR_KEYBOARD -> SuborKeyboard(console, keyMapping)
            BARCODE_BATTLER -> BarcodeBattlerReader(console)
            else -> return null
        }

        controlManagerListeners.forEach { it.onControlDeviceChange(console, device, port) }

        LOG.info("{} connected. type={}, port={}", device::class.simpleName, device.type, device.port)

        return device
    }

    fun processEndOfFrame() {
        if (!wasInputRead) {
            lagCounter++
        }

        wasInputRead = false
    }

    fun updateInputState() {
        console.keyManager.refreshKeyState()

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

    protected open fun remapControllerButtons() = Unit

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
        ranges.addHandler(READ, 0x4016, 0x4017)
        ranges.addHandler(WRITE, 0x4016)
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

    fun controlDevice(port: Int, subPort: Int): ControlDevice? {
        val device = controlDevice(port)
        return if (device is ControllerHub) device.controlDevice(subPort)
        else device
    }

    fun registerControlDevice(device: ControlDevice) {
        controlDevices.add(device)
    }

    fun hasControlDevice(type: ControllerType): Boolean {
        return controlDevices.any { it.hasControllerType(type) }
    }

    override fun saveState(s: Snapshot) {
        s.write("lagCounter", lagCounter)
        s.write("pollCounter", pollCounter)

        controlDevices.forEachIndexed { i, c -> s.write("controlDevice$i", c) }
    }

    override fun restoreState(s: Snapshot) {
        lagCounter = s.readInt("lagCounter")
        pollCounter = s.readInt("pollCounter")

        controlDevices.forEachIndexed { i, c -> s.readSnapshotable("controlDevice$i", c) }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(ControlManager::class.java)
    }
}
