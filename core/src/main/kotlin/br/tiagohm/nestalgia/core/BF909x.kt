package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_071

class BF909x : Mapper() {

    private var bf9097Mode = false

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        if (info.subMapperId == 1) {
            bf9097Mode = true
        }

        // First and last PRG page.
        selectPrgPage(0, 0)
        selectPrgPage(1, -1)

        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x9000) {
            // Firehawk uses $9000 to change mirroring.
            bf9097Mode = true
        }

        if (addr >= 0xC000 || !bf9097Mode) {
            selectPrgPage(0, value)
        } else {
            mirroringType = if (value.bit4) SCREEN_A_ONLY else SCREEN_B_ONLY
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("bf9097Mode", bf9097Mode)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        bf9097Mode = s.readBoolean("bf9097Mode")
    }
}
