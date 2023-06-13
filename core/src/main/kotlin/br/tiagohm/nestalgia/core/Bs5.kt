package br.tiagohm.nestalgia.core

class Bs5(console: Console) : Mapper(console) {

    override val dipSwitchCount = 2

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x800

    override fun initialize() {
        repeat(4) {
            selectPrgPage(it, -1)
            selectChrPage(it, -1)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        val bank = addr shr 10 and 0x03

        when (addr and 0xF000) {
            0x8000 -> selectChrPage(bank, addr and 0x1F)
            0xA000 -> if (addr and (1 shl dipSwitches + 4) != 0) {
                selectPrgPage(bank, addr and 0x0F)
            }
        }
    }
}
