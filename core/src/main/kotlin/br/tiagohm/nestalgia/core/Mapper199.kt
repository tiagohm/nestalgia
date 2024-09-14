package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.RAM
import br.tiagohm.nestalgia.core.ChrMemoryType.ROM
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_199

class Mapper199(console: Console) : MMC3(console) {

    private val exReg = IntArray(4)

    override val chrRamSize = 0x2000

    override val chrRamPageSize = 0x400

    override fun initialize() {
        resetExReg()

        super.initialize()
    }

    private fun resetExReg() {
        exReg[0] = 0xFE
        exReg[1] = 0xFF
        exReg[2] = 1
        exReg[3] = 3
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x8001 && state.reg8000.bit3) {
            exReg[state.reg8000 and 0x03] = value
            updatePrgMapping()
            updateChrMapping()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun updateMirroring() {
        mirroringType = when (state.regA000 and 0x03) {
            0 -> VERTICAL
            1 -> HORIZONTAL
            2 -> SCREEN_A_ONLY
            else -> SCREEN_B_ONLY
        }
    }

    override fun updatePrgMapping() {
        super.updatePrgMapping()
        selectPrgPage(2, exReg[0])
        selectPrgPage(3, exReg[1])
    }

    private fun chrMemoryType(value: Int): ChrMemoryType {
        return if (value < 8) RAM else ROM
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, page, chrMemoryType(page))
        super.selectChrPage(0, registers[0], chrMemoryType(registers[0]))
        super.selectChrPage(1, exReg[2], chrMemoryType(exReg[2]))
        super.selectChrPage(2, registers[1], chrMemoryType(registers[1]))
        super.selectChrPage(3, exReg[3], chrMemoryType(exReg[3]))
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exReg", exReg) ?: resetExReg()
    }
}
