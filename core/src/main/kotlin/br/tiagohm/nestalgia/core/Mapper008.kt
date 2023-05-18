package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_008

class Mapper008 : FrontFareast() {

    override fun init() {
        super.init()

        addRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.WRITE)
        selectPrgPage4x(0U, 0U)
    }

    override fun handleWriteRegister(addr: UShort, value: UByte) {
        if (addr >= 0x8000U) {
            selectPrgPage2x(0U, (value and 0xF8U).toUShort() shr 2)
            selectChrPage8x(0U, (value and 0x07U).toUShort() shl 3)
        }
    }
}
