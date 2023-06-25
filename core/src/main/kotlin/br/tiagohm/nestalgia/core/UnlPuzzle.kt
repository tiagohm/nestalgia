package br.tiagohm.nestalgia.core

class UnlPuzzle(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value shr 3 and 0x01)
        selectChrPage(0, value and 0x07)
    }
}
