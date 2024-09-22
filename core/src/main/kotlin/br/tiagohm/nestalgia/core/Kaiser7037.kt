package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE
import br.tiagohm.nestalgia.core.PrgMemoryType.ROM
import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_307

class Kaiser7037(console: Console) : Mapper(console) {

    override val workRamPageSize = 0x1000

    override val prgPageSize = 0x1000

    override val chrPageSize = 0x2000

    @Volatile private var currentRegister = 0
    private val regs = IntArray(8)

    override fun initialize() {
        selectChrPage(0, 0)
        removeRegisterRange(0xA000, 0xBFFF, READ_WRITE)

        updateState()
    }

    private fun updateState() {
        addCpuMemoryMapping(0x6000, 0x6FFF, 0, WRAM)
        addCpuMemoryMapping(0x7000, 0x7FFF, 15, ROM)
        selectPrgPage2x(0, regs[6] shl 1)
        selectPrgPage(2, -4)
        selectPrgPage(3, 1, WRAM)
        selectPrgPage2x(2, regs[7] shl 1)
        selectPrgPage2x(3, -2)
        nametables(regs[2] and 1, regs[4] and 1, regs[3] and 1, regs[5] and 1)
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
