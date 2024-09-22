package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_434

class BmcS2009(console: Console) : Mapper(console) {

    @Volatile private var prgBank = 0
    @Volatile private var outerBank = 0

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        addRegisterRange(0x6000, 0x7FFF, MemoryAccessType.WRITE)
        updateState()
    }

    override fun reset(softReset: Boolean) {
        prgBank = 0
        outerBank = 0
        updateState()
    }

    private fun updateState() {
        selectPrgPage(0, (outerBank shl 3) or (prgBank and 0x07))
        selectPrgPage(1, outerBank shl 3 or 0x07)
        selectChrPage(0, 0)
        mirroringType = if (outerBank.bit5) VERTICAL else HORIZONTAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            outerBank = value
        } else {
            prgBank = value
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgBank", prgBank)
        s.write("outerBank", outerBank)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgBank = s.readInt("prgBank")
        outerBank = s.readInt("outerBank")
    }
}
