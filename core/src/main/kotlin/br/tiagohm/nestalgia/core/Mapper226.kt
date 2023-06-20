package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_226

open class Mapper226(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    @JvmField protected val registers = IntArray(2)

    override fun initialize() {
        reset(true)
    }

    override fun reset(softReset: Boolean) {
        if (softReset) {
            registers.fill(0)

            selectPrgPage(0, 0)
            selectPrgPage(1, 1)
            selectChrPage(0, 0)
        }
    }

    protected open fun prgPage(): Int {
        return registers[0] and 0x1F or (registers[0] and 0x80 shr 2) or (registers[1] and 0x01 shl 6)
    }

    protected fun updatePrg() {
        val prgPage = prgPage()

        if (registers[0].bit5) {
            selectPrgPage(0, prgPage)
            selectPrgPage(1, prgPage)
        } else {
            selectPrgPage(0, prgPage and 0xFE)
            selectPrgPage(1, (prgPage and 0xFE) + 1)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0x8001) {
            0x8000 -> registers[0] = value
            0x8001 -> registers[1] = value
        }

        updatePrg()

        mirroringType = if (registers[0].bit6) VERTICAL else HORIZONTAL
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("registers", registers)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("registers", registers)
    }
}
