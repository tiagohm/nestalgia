package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL
import br.tiagohm.nestalgia.core.PrgMemoryType.ROM
import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM

// https://wiki.nesdev.com/w/index.php/INES_Mapper_103

class Mapper103(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val workRamSize = 0x4000

    override val workRamPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    @Volatile private var prgRamDisabled = false
    @Volatile private var prgReg = 0

    override fun initialize() {
        selectChrPage(0, 0)
        updateState()
    }

    private fun updateState() {
        selectPrgPage4x(0, -4)

        if (prgRamDisabled) {
            addCpuMemoryMapping(0x6000, 0x7FFF, prgReg, ROM)
        } else {
            addCpuMemoryMapping(0x6000, 0x7FFF, 0, WRAM)
            addCpuMemoryMapping(0xB800, 0xD7FF, 1, WRAM)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF000) {
            // Workram is always writeable, even when PRG ROM is mapped to $6000.
            0x6000, 0x7000 -> workRam[addr - 0x6000] = value
            0x8000 -> {
                prgReg = value and 0x0F
                updateState()
            }
            // Workram is always writeable, even when PRG ROM is mapped to $B800-$D7FF.
            0xB000, 0xC000, 0xD000 -> if (addr in 0xB800..0xd7ff) {
                workRam[0x2000 + addr - 0xB800] = value
            }
            0xE000 -> mirroringType = if (value.bit3) HORIZONTAL else VERTICAL
            0xF000 -> {
                prgRamDisabled = value.bit4
                updateState()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgRamDisabled", prgRamDisabled)
        s.write("prgReg", prgReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgRamDisabled = s.readBoolean("prgRamDisabled")
        prgReg = s.readInt("prgReg")

        updateState()
    }
}
