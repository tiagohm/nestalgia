package br.tiagohm.nestalgia.core

abstract class CNROM : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, powerOnByte())
    }
}
