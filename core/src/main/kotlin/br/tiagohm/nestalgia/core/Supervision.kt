package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_053

class Supervision(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    @Volatile private var epromFirst = false
    private val regs = IntArray(2)

    override fun initialize() {
        epromFirst = mPrgSize >= 0x8000 && prgRom.crc32(0..<0x8000) == 0x63794E25L

        updateState()
    }

    private fun updateState() {
        val r = regs[0] shl 3 and 0x78

        addCpuMemoryMapping(0x6000, 0x7FFF, (r shl 1 or 0x0F) + if (epromFirst) 0x04 else 0x00, ROM)

        selectPrgPage2x(0, (if (regs[0].bit4) (r or (regs[1] and 0x07)) + (if (epromFirst) 0x02 else 0x00) else if (epromFirst) 0x00 else 0x80) shl 1)
        selectPrgPage2x(1, (if (regs[0].bit4) (r or (0xFF and 0x07)) + (if (epromFirst) 0x02 else 0x00) else if (epromFirst) 0x01 else 0x81) shl 1)

        mirroringType = if (regs[0].bit5) HORIZONTAL else VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            regs[0] = value
        } else {
            regs[1] = value
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
    }
}
