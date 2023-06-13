package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_162

class Waixing162(console: Console) : Mapper(console) {

    private val exReg = IntArray(4)

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0x5FFF

    override fun initialize() {
        resetExReg()
        selectChrPage(0, 0)
        updateState()
    }

    private fun resetExReg() {
        exReg[0] = 3
        exReg[1] = 0
        exReg[2] = 0
        exReg[3] = 7
    }

    private fun updateState() {
        when (exReg[3] and 0x05) {
            0 -> {
                val page = (exReg[0] and 0x0C) or
                    (exReg[1] and 0x02) or
                    (exReg[2] and 0x0F shl 4)
                selectPrgPage(0, page)
            }
            1 -> {
                val page = (exReg[0] and 0x0C) or (exReg[2] and 0x0F shl 4)
                selectPrgPage(0, page)
            }
            4 -> {
                val page = (exReg[0] and 0x0E) or
                    (exReg[1] shr 1 and 0x01) or
                    (exReg[2] and 0x0F shl 4)
                selectPrgPage(0, page)
            }
            5 -> {
                val page = (exReg[0] and 0x0F) or (exReg[2] and 0x0F shl 4)
                selectPrgPage(0, page)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        exReg[addr shr 8 and 0x03] = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exReg", exReg) ?: resetExReg()
    }
}
