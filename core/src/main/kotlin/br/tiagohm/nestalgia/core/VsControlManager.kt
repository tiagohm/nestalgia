package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class VsControlManager(
    console: Console,
    systemActionManager: ControlDevice,
    mapperControlDevice: ControlDevice?,
) : ControlManager(console, systemActionManager, mapperControlDevice),
    InputProvider {

    private var protectionCounter = 0U
    private lateinit var vsSystemType: VsSystemType
    private var prgChrSelectBit: UByte = 0U
    private var slaveMasterBit: UByte = 0U
    private var refreshState = false

    override fun getControllerType(port: Int): ControllerType {
        val type = super.getControllerType(port)
        return if (type == ControllerType.ZAPPER) ControllerType.VS_ZAPPER else type
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        protectionCounter = 0U

        // Unsure about this, needed for VS Wrecking Crew
        updateSlaveMasterBit(if (console.isMaster) 0x00U else 0x02U)

        vsSystemType = console.mapper!!.info.vsType

        if (!softReset &&
            !console.isMaster &&
            console.dualConsole != null
        ) {
            unregisterInputProvider(this)
            registerInputProvider(this)
        }
    }

    override fun dispose() {
        unregisterInputProvider(this)
    }

    override fun setInput(device: ControlDevice): Boolean {
        val port = device.port
        val masterControlManager = console.dualConsole?.controlManager

        if (masterControlManager != null && port <= 1) {
            val controlDevice = masterControlManager.getControlDevice(port + 2)
            controlDevice?.state?.state?.copyInto(device.state.state)
        }

        return true
    }

    override fun remapControllerButtons() {
        // TODO:
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        super.getMemoryRanges(ranges)
        ranges.addHandler(MemoryOperation.READ, 0x4020U, 0x5FFFU)
        ranges.addHandler(MemoryOperation.WRITE, 0x4020U, 0x5FFFU)
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        super.write(addr, value, type)

        refreshState = value.bit0

        if (addr.toUInt() == 0x4016U) {
            prgChrSelectBit = (value shr 2) and 0x01U

            // Bit 2: DualSystem-only
            val bit = value and 0x02U

            if (bit != slaveMasterBit) {
                updateSlaveMasterBit(bit)
            }
        }
    }

    override fun getOpenBusMask(port: UByte): UByte {
        return 0x00U
    }

    private fun updateSlaveMasterBit(bit: UByte) {
        val dualConsole = console.dualConsole

        if (dualConsole != null) {
            val mapper = console.mapper!! as VsSystem

            if (console.isMaster) {
                mapper.updateMemoryAccess(bit)
            }

            if (bit.toUInt() != 0U) {
                dualConsole.cpu.clearIRQSource(IRQSource.EXTERNAL)
            } else {
                // When low, asserts /IRQ on the other CPU
                dualConsole.cpu.setIRQSource(IRQSource.EXTERNAL)
            }
        }

        slaveMasterBit = bit
    }
}