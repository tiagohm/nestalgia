package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_221

class Mapper221(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private var mode = 0
    private var prgReg = 0

    override fun initialize() {
        selectChrPage(0, 0)

        updateState()
    }

    private fun updateState() {
        val outerBank = mode and 0xFC shr 2

        if (mode.bit1) {
            if (mode.bit8) {
                selectPrgPage(0, outerBank or prgReg)
                selectPrgPage(1, outerBank or 0x07)
            } else {
                selectPrgPage2x(0, outerBank or (prgReg and 0x06))
            }
        } else {
            selectPrgPage(0, outerBank or prgReg)
            selectPrgPage(1, outerBank or prgReg)
        }

        mirroringType = if (mode.bit0) HORIZONTAL else VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xC000) {
            0x8000 -> {
                mode = addr
                updateState()
            }
            0xC000 -> {
                prgReg = addr and 0x07
                updateState()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("mode", mode)
        s.write("prgReg", prgReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        mode = s.readInt("mode")
        prgReg = s.readInt("prgReg")
    }
}
