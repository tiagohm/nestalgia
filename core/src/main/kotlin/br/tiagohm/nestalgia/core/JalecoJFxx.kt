package br.tiagohm.nestalgia.core

abstract class JalecoJFxx : Mapper() {

    final override val prgPageSize = 0x8000

    final override val chrPageSize = 0x2000

    final override val registerStartAddress = 0x6000

    final override val registerEndAddress = 0x7FFF

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    abstract override fun writeRegister(addr: Int, value: Int)
}
