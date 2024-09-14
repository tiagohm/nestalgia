package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_051

class Bmc51(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    @Volatile private var bank = 0
    @Volatile private var mode = 1

    override fun initialize() {
        updateState()
    }

    private fun updateState() {
        if (mode.bit0) {
            selectPrgPage4x(0, bank shl 2)
            addCpuMemoryMapping(0x6000, 0x7FFF, 0x23 or (bank shl 2), ROM)
        } else {
            selectPrgPage2x(0, bank shl 2 or mode)
            selectPrgPage2x(1, bank shl 2 or 0x0E)
            addCpuMemoryMapping(0x6000, 0x7FFF, 0x2F or (bank shl 2), ROM)
        }

        mirroringType = if (mode == 0x03) HORIZONTAL else VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0x7FFF) {
            mode = value shr 3 and 0x02 or (value shr 1 and 0x01)
        } else if (addr in 0xC000..0xDFFF) {
            bank = value and 0x0F
            mode = value shr 3 and 0x02 or (mode and 0x01)
        } else {
            bank = value and 0x0F
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("bank", bank)
        s.write("mode", mode)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        bank = s.readInt("bank")
        mode = s.readInt("mode", 1)

        updateState()
    }
}
