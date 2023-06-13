package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_164

class Waixing164(console: Console) : Mapper(console) {

    private var prgBank = 0

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0x5FFF

    override fun initialize() {
        prgBank = 0x0F
        selectPrgPage(0, prgBank)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0x7300) {
            0x5000 -> {
                prgBank = (prgBank and 0xF0) or (value and 0x0F)
                selectPrgPage(0, prgBank)
            }
            0x5100 -> {
                prgBank = (prgBank and 0x0F) or (value and 0x0F shl 4)
                selectPrgPage(0, prgBank)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgBank", prgBank)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgBank = s.readInt("prgBank", 0x0F)
    }
}
