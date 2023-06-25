package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_332

class Super40in1Ws(console: Console) : Mapper(console) {

    private var regLock = false

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0x6FFF

    override fun initialize() {
        writeRegister(0x6000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (!regLock) {
            if (addr.bit0) {
                selectChrPage(0, value)
            } else {
                regLock = value.bit5
                selectPrgPage(0, value and (value.inv() shr 3 and 0x01).inv())
                selectPrgPage(1, value or (value.inv() shr 3 and 0x01))
                mirroringType = if (value.bit4) HORIZONTAL else VERTICAL
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regLock", regLock)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        regLock = s.readBoolean("regLock")
    }
}
