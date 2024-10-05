package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_400

class Mapper400(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private val regs = IntArray(2)
    @Volatile private var led = 0

    override fun initialize() {
        addRegisterRange(0x7800, 0x7FFF, MemoryAccessType.WRITE)
        reset()
    }

    override fun reset(softReset: Boolean) {
        regs[0] = 0x80
        writeRegister(0xC000, 0)
    }

    private fun updateState() {
        selectPrgPage(0, (regs[0] and 0xF8) or (regs[1] and 0x07))
        selectPrgPage(1, (regs[0] and 0xF8) or 0x07)

        selectChrPage(0, regs[1] shr 5)

        if (regs[0] != 0x80) {
            mirroringType = if (regs[0].bit5) HORIZONTAL else VERTICAL
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            regs[0] = value
            updateState()
        } else {
            if (addr < 0xC000) {
                led = value
            } else {
                regs[1] = value
                updateState()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
        s.write("led", led)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
        led = s.readInt("led")
    }
}
