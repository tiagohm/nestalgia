package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_091

class Mapper091 : MMC3() {

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0x7FFF

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x800

    override fun initialize() {
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)
    }

    override fun updateState() {
        // Do nothing, we are only using MMC3 code to emulate the IRQs
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0x7003) {
            0x6000 -> selectChrPage(0, value)
            0x6001 -> selectChrPage(1, value)
            0x6002 -> selectChrPage(2, value)
            0x6003 -> selectChrPage(3, value)
            0x7000 -> selectPrgPage(0, value and 0x0F)
            0x7001 -> selectPrgPage(1, value and 0x0F)
            0x7002 -> super.writeRegister(0xE000, value)
            0x7003 -> {
                super.writeRegister(0xC000, 0x07)
                super.writeRegister(0xC001, value)
                super.writeRegister(0xE001, value)
            }
        }
    }
}
