package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_091

class Mapper091 : MMC3() {

    override val registerStartAddress: UShort = 0x6000U

    override val registerEndAddress: UShort = 0x7FFFU

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x800U

    override fun init() {
        selectPrgPage(2U, 0xFFFEU)
        selectPrgPage(3U, 0xFFFFU)
    }

    override fun updateState() {
        // Do nothing, we are only using MMC3 code to emulate the IRQs
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        when (addr.toInt() and 0x7003) {
            0x6000 -> selectChrPage(0U, value.toUShort())
            0x6001 -> selectChrPage(1U, value.toUShort())
            0x6002 -> selectChrPage(2U, value.toUShort())
            0x6003 -> selectChrPage(3U, value.toUShort())
            0x7000 -> selectPrgPage(0U, (value and 0x0FU).toUShort())
            0x7001 -> selectPrgPage(1U, (value and 0x0FU).toUShort())
            0x7002 -> super.writeRegister(0xE000U, value)
            0x7003 -> {
                super.writeRegister(0xC000U, 0x07U)
                super.writeRegister(0xC001U, value)
                super.writeRegister(0xE001U, value)
            }
        }
    }
}