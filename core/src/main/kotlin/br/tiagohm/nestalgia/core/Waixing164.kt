package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_164

class Waixing164 : Mapper() {

    private var prgBank: UShort = 0U

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x5000U

    override val registerEndAddress: UShort = 0x5FFFU

    override fun init() {
        prgBank = 0x0FU
        selectPrgPage(0U, prgBank)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        when (addr.toInt() and 0x7300) {
            0x5000 -> {
                prgBank = (prgBank and 0xF0U) or (value and 0x0FU).toUShort()
                selectPrgPage(0U, prgBank)
            }
            0x5100 -> {
                prgBank = (prgBank and 0x0FU) or ((value and 0x0FU).toUInt() shl 4).toUShort()
                selectPrgPage(0U, prgBank)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgBank", prgBank)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgBank = s.readUShort("prgBank") ?: 0x0FU
    }
}
