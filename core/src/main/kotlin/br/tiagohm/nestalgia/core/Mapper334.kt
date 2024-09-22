package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_334

class Mapper334(console: Console) : MMC3(console) {

    override val dipSwitchCount = 1

    override val allowRegisterRead = true

    override fun initialize() {
        super.initialize()

        selectPrgPage4x(0, -4)
        selectChrPage8x(0, 0)
        addRegisterRange(0x6000, 0x7FFF, MemoryAccessType.READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, MemoryAccessType.READ)
    }

    override fun updatePrgMapping() = Unit

    override fun readRegister(addr: Int): Int {
        var value = console.memoryManager.openBus()

        if (addr.bit1) {
            value = (value and 0xFE) or (dipSwitches and 0x01)
        }

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (!addr.bit0) {
                selectPrgPage4x(0, value and 0xFE shl 1)
            }
        } else {
            super.writeRegister(addr, value)
        }
    }
}
