package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_059

class UnlD1038(console: Console) : Mapper(console) {

    @Volatile private var returnDipSwitch = false

    override val dipSwitchCount = 2

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun readRegister(addr: Int): Int {
        return if (returnDipSwitch) dipSwitches else internalRead(addr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr.bit7) {
            selectPrgPage(0, (addr and 0x70) shr 4)
            selectPrgPage(1, (addr and 0x70) shr 4)
        } else {
            selectPrgPage2x(0, (addr and 0x60) shr 4)
        }

        selectChrPage(0, addr and 0x07)

        mirroringType = if (addr.bit3) HORIZONTAL else VERTICAL
        returnDipSwitch = (addr and 0x100) == 0x100
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("returnDipSwitch", returnDipSwitch)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        returnDipSwitch = s.readBoolean("returnDipSwitch")
    }
}
