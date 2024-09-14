package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_522

class Lh10(console: Console) : Mapper(console) {

    @Volatile private var currentRegister = 0
    private val regs = IntArray(8)

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectChrPage(0, 0)
        removeRegisterRange(0xC000, 0xDFFF, READ_WRITE)

        updateState()
    }

    private fun updateState() {
        addCpuMemoryMapping(0x6000, 0x7FFF, -2, ROM)
        selectPrgPage(0, regs[6])
        selectPrgPage(1, regs[7])
        selectPrgPage(2, 0, WRAM)
        selectPrgPage(3, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE001) {
            0x8000 -> currentRegister = value and 0x07
            0x8001 -> {
                regs[currentRegister] = value
                updateState()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("currentRegister", currentRegister)
        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        currentRegister = s.readInt("currentRegister")
        s.readIntArray("regs", regs)

        updateState()
    }
}
