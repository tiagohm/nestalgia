package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_226

class Mapper226(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private val registers = IntArray(2)

    override fun initialize() = Unit

    override fun reset(softReset: Boolean) {
        registers.fill(0)
        updateState()
    }

    private fun prgPage(): Int {
        var base = (registers[0] and 0x80 shr 7) or (registers[1] and 0x01 shl 1)

        //  for 1536 KB PRG roms / BMC-Ghostbusters63in1
        if (mPrgSize == 1536 * 1024) {
            base = BANKS[base]
        }

        return (registers[0] and 0x1F) or (base shl 5)
    }

    private fun updateState() {
        val prgPage = prgPage()

        if (registers[0].bit5) {
            selectPrgPage(0, prgPage)
            selectPrgPage(1, prgPage)
        } else {
            selectPrgPage(0, prgPage and 0xFE)
            selectPrgPage(1, (prgPage and 0xFE) + 1)
        }

        selectChrPage(0, 0)

        mirroringType = if (registers[0].bit6) VERTICAL else HORIZONTAL

    }

    override fun writeRegister(addr: Int, value: Int) {
        registers[addr and 0x01] = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("registers", registers)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("registers", registers)
    }

    companion object {

        private val BANKS = arrayOf(0, 0, 1, 2)
    }
}
