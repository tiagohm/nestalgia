package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_080
// https://wiki.nesdev.com/w/index.php/INES_Mapper_207

class TaitoX1005(console: Console, private val alternateMirroring: Boolean) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override val registerStartAddress = 0x7EF0

    override val registerEndAddress = 0x7EFF

    override val workRamSize = 0x100

    override val workRamPageSize = 0x100

    override val saveRamSize = 0x100

    override val saveRamPageSize = 0x100

    @Volatile private var ramPermission = 0

    override fun initialize() {
        selectPrgPage(3, -1)

        updateRamAccess()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateRamAccess() {
        addCpuMemoryMapping(0x7F00, 0x7FFF, 0, if (hasBattery) SRAM else WRAM, if (ramPermission == 0xA3) READ_WRITE else NO_ACCESS)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0x7EF0 -> {
                selectChrPage(0, value)
                selectChrPage(1, value + 1)

                if (alternateMirroring) {
                    nametable(0, value shr 7)
                    nametable(1, value shr 7)
                }
            }
            0x7EF1 -> {
                selectChrPage(2, value)
                selectChrPage(3, value + 1)

                if (alternateMirroring) {
                    nametable(2, value shr 7)
                    nametable(3, value shr 7)
                }
            }
            0x7EF2 -> selectChrPage(4, value)
            0x7EF3 -> selectChrPage(5, value)
            0x7EF4 -> selectChrPage(6, value)
            0x7EF5 -> selectChrPage(7, value)
            0x7EF6, 0x7EF7 -> if (!alternateMirroring) {
                mirroringType = if (value.bit0) VERTICAL else HORIZONTAL
            }
            0x7EF8, 0x7EF9 -> {
                ramPermission = value
                updateRamAccess()
            }
            0x7EFA, 0x7EFB -> selectPrgPage(0, value)
            0x7EFC, 0x7EFD -> selectPrgPage(1, value)
            0x7EFE, 0x7EFF -> selectPrgPage(2, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("ramPermission", ramPermission)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        ramPermission = s.readInt("ramPermission")

        updateRamAccess()
    }
}
