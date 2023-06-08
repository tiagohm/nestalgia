package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_000

open class NROM : Mapper() {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 1)

        selectChrPage(0, 0)
    }
}
