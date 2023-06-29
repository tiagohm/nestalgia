package br.tiagohm.nestalgia.core

class Unl255in1(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectChrPage(0, addr and 0x07)
        selectPrgPage(0, addr shr 2 and 0x03)
    }
}
