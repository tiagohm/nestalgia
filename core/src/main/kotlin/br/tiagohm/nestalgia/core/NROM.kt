package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_000

@ExperimentalUnsignedTypes
open class NROM : Mapper() {

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x2000U

    override fun init() {
        selectPrgPage(0U, 0U)
        selectPrgPage(1U, 1U)

        selectChrPage(0U, 0U)
    }
}