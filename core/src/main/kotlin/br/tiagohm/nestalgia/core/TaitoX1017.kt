package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_082

class TaitoX1017(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override val registerStartAddress = 0x7EF0

    override val registerEndAddress = 0x7EFF

    override val saveRamSize = 0x1400

    override val saveRamPageSize = 0x400

    private val chrRegs = IntArray(6)
    private val ramPermission = IntArray(3)
    @Volatile private var chrMode = 0

    override fun initialize() {
        selectPrgPage(3, -1)

        updateRamAccess()
    }

    private fun updateRamAccess() {
        addCpuMemoryMapping(0x6000, 0x63FF, 0, SRAM, if (ramPermission[0] == 0xCA) READ_WRITE else NO_ACCESS)
        addCpuMemoryMapping(0x6400, 0x67FF, 1, SRAM, if (ramPermission[0] == 0xCA) READ_WRITE else NO_ACCESS)
        addCpuMemoryMapping(0x6800, 0x6BFF, 2, SRAM, if (ramPermission[1] == 0x69) READ_WRITE else NO_ACCESS)
        addCpuMemoryMapping(0x6C00, 0x6FFF, 3, SRAM, if (ramPermission[1] == 0x69) READ_WRITE else NO_ACCESS)
        addCpuMemoryMapping(0x7000, 0x73FF, 4, SRAM, if (ramPermission[2] == 0x84) READ_WRITE else NO_ACCESS)
    }

    private fun updateChrBanking() {
        if (chrMode == 0) {
            // Regs 0 & 1 ignore the LSB.
            selectChrPage2x(0, chrRegs[0] and 0xFE)
            selectChrPage2x(1, chrRegs[1] and 0xFE)
            selectChrPage(4, chrRegs[2])
            selectChrPage(5, chrRegs[3])
            selectChrPage(6, chrRegs[4])
            selectChrPage(7, chrRegs[5])
        } else {
            selectChrPage(0, chrRegs[2])
            selectChrPage(1, chrRegs[3])
            selectChrPage(2, chrRegs[4])
            selectChrPage(3, chrRegs[5])

            // Regs 0 & 1 ignore the LSB.
            selectChrPage2x(2, chrRegs[0] and 0xFE)
            selectChrPage2x(3, chrRegs[1] and 0xFE)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0x7EF0, 0x7EF1, 0x7EF2, 0x7EF3, 0x7EF4, 0x7EF5 -> {
                chrRegs[addr and 0xF] = value
                updateChrBanking()
            }
            0x7EF6 -> {
                mirroringType = if (value.bit0) VERTICAL else HORIZONTAL
                chrMode = value and 0x02 shr 1
                updateChrBanking()
            }
            0x7EF7, 0x7EF8, 0x7EF9 -> {
                ramPermission[(addr and 0xF) - 7] = value
                updateRamAccess()
            }
            0x7EFA -> selectPrgPage(0, value shr 2)
            0x7EFB -> selectPrgPage(1, value shr 2)
            0x7EFC -> selectPrgPage(2, value shr 2)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrRegs", chrRegs)
        s.write("ramPermission", ramPermission)
        s.write("chrMode", chrMode)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("chrRegs", chrRegs)
        s.readIntArray("ramPermission", ramPermission)
        chrMode = s.readInt("chrMode")

        updateRamAccess()
    }
}
