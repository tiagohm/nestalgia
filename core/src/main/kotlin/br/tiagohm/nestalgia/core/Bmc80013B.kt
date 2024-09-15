package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_274

class Bmc80013B(console: Console) : Mapper(console) {

    private val regs = IntArray(2)
    @Volatile private var mode = 0

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        regs.fill(0)
        mode = 0
        updateState()
    }

    private fun updateState() {
        if (mode.bit1) {
            selectPrgPage(0, (regs[0] and 0x0F) or (regs[1] and 0x70))
        } else {
            selectPrgPage(0, regs[0] and 0x03)
        }

        selectPrgPage(1, regs[1] and 0x7F)

        mirroringType = if (regs[0].bit4) VERTICAL else HORIZONTAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        val reg = addr shr 13 and 0x03

        if (reg == 0) {
            regs[0] = value
        } else {
            regs[1] = value
            mode = reg
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
        s.write("mode", mode)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
        mode = s.readInt("mode")
    }
}
