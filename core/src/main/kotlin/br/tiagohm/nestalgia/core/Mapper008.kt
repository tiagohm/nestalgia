package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE

// https://wiki.nesdev.com/w/index.php/INES_Mapper_008

class Mapper008(console: Console) : FrontFareast(console) {

    override fun initialize() {
        addRegisterRange(0x8000, 0xFFFF, WRITE)
        selectPrgPage4x(0, 0)
    }

    override fun internalWriteRegister(addr: Int, value: Int) {
        if (addr >= 0x8000) {
            selectPrgPage2x(0, value and 0xF8 shr 2)
            selectChrPage8x(0, value and 0x07 shl 3)
        }
    }
}
