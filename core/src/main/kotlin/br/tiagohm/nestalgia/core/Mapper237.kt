package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_237
// https://github.com/Erendel/Mesen/commit/c6ae2e1a533094343d050e2fd2985cb11e677fbe#diff-2133487c3e0c1dc0d4bf59347892dc61c54d8c9ea79086d974caf9d6dd28beffR65

class Mapper237(console: Console) : Mapper(console) {

    @Volatile private var prgBank = 0
    @Volatile private var type = false
    @Volatile private var lock = false

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    override val dipSwitchCount = 2

    override fun initialize() {
        reset()
    }

    override fun reset(softReset: Boolean) {
        lock = false
        writeRegister(0x8000, 0)
    }

    override fun readRegister(addr: Int): Int {
        return if (type) dipSwitches else internalRead(addr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (lock) {
            prgBank = prgBank and 0x07.inv()
            prgBank = prgBank or (value and 0x07)
        } else {
            type = addr.bit0
            lock = addr.bit1
            prgBank = (addr shl 3 and 0x20) or (value and 0x1F)
        }

        mirroringType = if (value.bit5) HORIZONTAL else VERTICAL

        if (value.bit7) {
            if (value.bit6) {
                selectPrgPage2x(0, prgBank and 0xFE)
            } else {
                selectPrgPage(0, prgBank)
                selectPrgPage(1, prgBank)
            }
        } else {
            selectPrgPage(0, prgBank)
            selectPrgPage(1, prgBank or 0x07)
        }

        selectChrPage(0, 0)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgBank", prgBank)
        s.write("type", type)
        s.write("lock", lock)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgBank = s.readInt("prgBank")
        type = s.readBoolean("type")
        lock = s.readBoolean("lock")
    }
}
