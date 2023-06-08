package br.tiagohm.nestalgia.core

class VsControlManager(
    console: Console,
    systemActionManager: ControlDevice,
    mapperControlDevice: ControlDevice?,
) : ControlManager(console, systemActionManager, mapperControlDevice),
    InputProvider {

    private var protectionCounter = 0
    private var vsSystemType = VsSystemType.DEFAULT
    private var prgChrSelectBit = 0
    private var slaveMasterBit = 0
    private var refreshState = false

    override fun controllerType(port: Int): ControllerType {
        val type = super.controllerType(port)
        return if (type == ControllerType.ZAPPER) ControllerType.VS_ZAPPER
        else type
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        protectionCounter = 0

        // Unsure about this, needed for VS Wrecking Crew
        updateSlaveMasterBit(if (console.isMaster) 0x00 else 0x02)

        vsSystemType = console.mapper!!.info.vsType

        if (!softReset &&
            !console.isMaster &&
            console.dualConsole != null
        ) {
            unregisterInputProvider(this)
            registerInputProvider(this)
        }
    }

    override fun close() {
        unregisterInputProvider(this)
    }

    override fun setInput(device: ControlDevice): Boolean {
        val port = device.port
        val masterControlManager = console.dualConsole?.controlManager

        if (masterControlManager != null && port <= 1) {
            val controlDevice = masterControlManager.controlDevice(port + 2)
            controlDevice?.state?.state?.copyInto(device.state.state)
        }

        return true
    }

    override fun remapControllerButtons() {
        // TODO:
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        super.memoryRanges(ranges)
        ranges.addHandler(MemoryOperation.READ, 0x4020, 0x5FFF)
        ranges.addHandler(MemoryOperation.WRITE, 0x4020, 0x5FFF)
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        super.write(addr, value, type)

        refreshState = value.bit0

        if (addr == 0x4016) {
            prgChrSelectBit = (value shr 2) and 0x01

            // Bit 2: DualSystem-only
            val bit = value and 0x02

            if (bit != slaveMasterBit) {
                updateSlaveMasterBit(bit)
            }
        }
    }

    override fun openBusMask(port: Int) = 0x00

    private fun updateSlaveMasterBit(bit: Int) {
        val dualConsole = console.dualConsole

        if (dualConsole != null) {
            val mapper = console.mapper!! as VsSystem

            if (console.isMaster) {
                mapper.updateMemoryAccess(bit)
            }

            if (bit != 0) {
                dualConsole.cpu.clearIRQSource(IRQSource.EXTERNAL)
            } else {
                // When low, asserts /IRQ on the other CPU
                dualConsole.cpu.setIRQSource(IRQSource.EXTERNAL)
            }
        }

        slaveMasterBit = bit
    }
}
