package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_006

class Mapper006(console: Console) : FrontFareast(console) {

    override fun initialize() {
        addRegisterRange(0x8000, 0xFFFF, WRITE)
        selectPrgPage2x(0, 0)
        selectPrgPage2x(1, 14)
    }

    override fun internalWriteRegister(addr: Int, value: Int) {
        if (addr >= 0x8000) {
            if (hasChrRam || ffeAltMode) {
                selectPrgPage2x(0, value and 0xFC shr 1)
                selectChrPage8x(0, value and 0x03 shl 3)
            } else {
                selectChrPage8x(0, value shl 3)
            }
        }
    }
}
