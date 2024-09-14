package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_519

class Eh8813A(console: Console) : Mapper(console) {

    override val dipSwitchCount = 4

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    @Volatile private var alterReadAddress = false

    override fun initialize() {
        mirroringType = VERTICAL
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
        alterReadAddress = false
    }

    override fun readRegister(addr: Int): Int {
        var newAddr = addr

        if (alterReadAddress) {
            newAddr = (addr and 0xFFF0) + dipSwitches
        }

        return internalRead(newAddr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x0100 == 0) {
            alterReadAddress = addr.bit6

            if (addr.bit7) {
                selectPrgPage(0, addr and 0x07)
                selectPrgPage(1, addr and 0x07)
            } else {
                selectPrgPage2x(0, addr and 0x06)
            }

            selectChrPage(0, value and 0x0F)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("alterReadAddress", alterReadAddress)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        alterReadAddress = s.readBoolean("alterReadAddress")
    }
}
