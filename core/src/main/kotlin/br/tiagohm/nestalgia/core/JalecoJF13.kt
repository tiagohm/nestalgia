package br.tiagohm.nestalgia.core


// https://wiki.nesdev.com/w/index.php/INES_Mapper_086

class JalecoJF13 : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0x7FFF

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0x7000) {
            0x6000 -> {
                selectPrgPage(0, value and 0x30 shr 4)
                selectChrPage(0, value and 0x03 or (value shr 4 and 0x04))
            }
            // Audio not supported.
            0x7000 -> Unit
        }
    }
}
