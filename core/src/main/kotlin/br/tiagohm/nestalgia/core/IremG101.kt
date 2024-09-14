package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_032

class IremG101(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    private val prgRegs = IntArray(2)
    @Volatile private var prgMode = 0

    override fun initialize() {
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)

        if (info.subMapperId == 1) {
            // 032: 1 Major League
            // CIRAM A10 is tied high (fixed one-screen mirroring) and PRG
            // banking style is fixed as 8+8+16F.
            mirroringType = SCREEN_A_ONLY
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updatePrgMode() {
        if (prgMode == 0) {
            selectPrgPage(0, prgRegs[0])
            selectPrgPage(1, prgRegs[1])
            selectPrgPage(2, -2)
            selectPrgPage(3, -1)
        } else {
            selectPrgPage(0, -2)
            selectPrgPage(1, prgRegs[1])
            selectPrgPage(2, prgRegs[0])
            selectPrgPage(3, -1)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF000) {
            0x8000 -> {
                prgRegs[0] = value and 0x1F
                selectPrgPage(if (prgMode == 0) 0 else 2, prgRegs[0])
            }
            0x9000 -> {
                prgMode = value and 0x02 shr 1

                if (info.subMapperId == 1) {
                    prgMode = 0
                }

                updatePrgMode()

                mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
            }
            0xA000 -> {
                prgRegs[1] = value and 0x1F
                selectPrgPage(1, prgRegs[1])
            }
            0xB000 -> selectChrPage(addr and 0x07, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgRegs", prgRegs)
        s.write("prgMode", prgMode)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("prgRegs", prgRegs)
        prgMode = s.readInt("prgMode")
    }
}
