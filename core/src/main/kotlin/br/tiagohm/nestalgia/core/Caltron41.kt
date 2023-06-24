package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperation.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_041

class Caltron41(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    private var prgBank = 0
    private var chrBank = 0

    override fun initialize() {
        addRegisterRange(0x6000, 0x67FF, WRITE)
    }

    override fun reset(softReset: Boolean) {
        chrBank = 0
        prgBank = 0
        writeRegister(0x6000, 0)
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0x67FF) {
            prgBank = addr and 0x07
            chrBank = chrBank and 0x03 or (addr shr 1 and 0x0C)
            selectPrgPage(0, prgBank)
            selectChrPage(0, chrBank)
            mirroringType = if (addr.bit5) HORIZONTAL else VERTICAL
        } else {
            // Note that the Inner CHR Bank select only can be written while
            // the PRG ROM bank is 4, 5, 6, or 7.
            if (prgBank >= 4) {
                chrBank = chrBank and 0x0C or (value and 0x03)
                selectChrPage(0, chrBank)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgBank", prgBank)
        s.write("chrBank", chrBank)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgBank = s.readInt("prgBank")
        chrBank = s.readInt("chrBank")
    }
}
