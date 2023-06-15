package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_150
// https://wiki.nesdev.com/w/index.php/INES_Mapper_243

class Sachen74LS374N(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0x7FFF

    override val allowRegisterRead = true

    override val dipSwitchCount
        get() = if (info.mapperId == 150) 1 else 0

    private var currentReg = 0
    private val regs = IntArray(8)

    override fun initialize() {
        updateState()
    }

    private fun updateState() {
        val chrPage = if (info.mapperId == 150) {
            (regs[4] and 0x01 shl 2) or (regs[6] and 0x03)
        } else {
            (regs[2] and 0x01) or (regs[4] and 0x01 shl 1) or (regs[6] and 0x03 shl 2)
        }

        selectChrPage(0, chrPage)
        selectPrgPage(0, regs[5] and 0x03)

        when (regs[7] shr 1 and 0x03) {
            0 -> nametables(0, 0, 0, 1)
            1 -> mirroringType = HORIZONTAL
            2 -> mirroringType = VERTICAL
            3 -> mirroringType = SCREEN_A_ONLY
        }
    }

    override fun readRegister(addr: Int): Int {
        val openBus = console.memoryManager.openBus()

        if (addr and 0xC101 == 0x4101) {
            return if (dipSwitches.bit0) {
                // In the latter setting, the ASIC sees all writes as being OR'd with $04,
                // while on reads, D2 is open bus.
                (openBus and 0xFC) or (regs[currentReg] and 0x03)
            } else {
                (openBus and 0xF8) or (regs[currentReg] and 0x07)
            }
        }

        return openBus
    }

    override fun writeRegister(addr: Int, value: Int) {
        var newValue = value

        if (dipSwitches.bit0) {
            // In the latter setting, the ASIC sees all writes as being OR'd with $04,
            // while on reads, D2 is open bus.
            newValue = value or 0x04
        }

        when (addr and 0xC101) {
            0x4100 -> currentReg = newValue and 0x07
            0x4101 -> {
                regs[currentReg] = newValue and 0x07
                updateState()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
        s.write("currentReg", currentReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
        currentReg = s.readInt("currentReg")
    }
}
