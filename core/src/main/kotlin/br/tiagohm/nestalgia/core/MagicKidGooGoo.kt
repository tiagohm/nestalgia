package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_190

class MagicKidGooGoo : Mapper() {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x800

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 0)

        selectChrPage4x(0, 0)

        mirroringType = MirroringType.VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        when {
            addr in 0x8000..0x9FFF -> selectPrgPage(0, value and 0x07)
            addr in 0xC000..0xDFFF -> selectPrgPage(0, (value and 0x07) or 0x08)
            (addr and 0xA000) == 0xA000 -> selectChrPage(addr and 0x03, value)
        }
    }
}
