package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_227

class Mapper227(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val prgBank = addr shr 2 and 0x1F or (addr and 0x100 shr 3)
        val sFlag = addr.bit0
        val lFlag = (addr shr 9).bit0
        val prgMode = (addr shr 7).bit0

        if (prgMode) {
            if (sFlag) {
                selectPrgPage2x(0, prgBank and 0xFE)
            } else {
                selectPrgPage(0, prgBank)
                selectPrgPage(1, prgBank)
            }
        } else if (sFlag) {
            if (lFlag) {
                selectPrgPage(0, prgBank and 0x3E)
                selectPrgPage(1, prgBank or 0x07)
            } else {
                selectPrgPage(0, prgBank and 0x3E)
                selectPrgPage(1, prgBank and 0x38)
            }
        } else if (lFlag) {
            selectPrgPage(0, prgBank)
            selectPrgPage(1, prgBank or 0x07)
        } else {
            selectPrgPage(0, prgBank)
            selectPrgPage(1, prgBank and 0x38)
        }

        mirroringType = if (addr.bit1) HORIZONTAL else VERTICAL
    }
}
