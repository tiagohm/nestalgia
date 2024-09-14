package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_202

class Mapper202(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    @Volatile private var prgMode1 = false

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        prgMode1 = addr and 0x09 == 0x09

        val page = addr shr 1 and 0x07

        selectChrPage(0, page)
        selectPrgPage(0, page)

        if (prgMode1) {
            selectPrgPage(1, page + 1)
        } else {
            selectPrgPage(1, page)
        }

        mirroringType = if (addr.bit0) HORIZONTAL else VERTICAL
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgMode1", prgMode1)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgMode1 = s.readBoolean("prgMode1")
    }
}
