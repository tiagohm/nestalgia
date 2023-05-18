package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_006

class Mapper006 : FrontFareast() {

    override fun init() {
        super.init()

        addRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.WRITE)
        selectPrgPage2x(0U, 0U)
        selectPrgPage2x(1U, 14U)
    }

    override fun handleWriteRegister(addr: UShort, value: UByte) {
        if (addr >= 0x8000U) {
            if (hasChrRam || ffeAltMode) {
                selectPrgPage2x(0U, (value.toUShort() and 0xFCU) shr 1)
                selectChrPage8x(0U, (value and 0x03U).toUShort() shl 3)
            } else {
                selectChrPage8x(0U, value.toUShort() shl 3)
            }
        }
    }
}
