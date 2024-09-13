package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

class Ac08(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    @Volatile private var reg = 0

    override fun initialize() {
        addRegisterRange(0x4025, 0x4025, WRITE)

        selectPrgPage4x(0, -4)
        selectChrPage(0, 0)
        updateState()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateState() {
        addCpuMemoryMapping(0x6000, 0x7FFF, reg, ROM)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x4025) {
            mirroringType = if (value.bit3) HORIZONTAL else VERTICAL
        } else {
            reg = if (addr == 0x8001) {
                // Green beret.
                value shr 1 and 0x0F
            } else {
                // Castlevania?
                value and 0x0F
            }

            updateState()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("reg", reg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        reg = s.readInt("reg")

        updateState()
    }
}
