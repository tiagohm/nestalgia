package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_414

class Mapper414(console: Console) : Mapper(console) {

    @Volatile private var dipSwitch = 0
    @Volatile private var latch = 0

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    override val hasBusConflicts = true

    override val dipSwitchCount = 3

    override fun initialize() {
        val index = dipSwitches.let { if (it > 4) 0 else it }
        dipSwitch = DPSWLUT[index]
        reset()
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
    }

    override fun readRegister(addr: Int): Int {
        if (addr >= 0xC000) {
            if (!latch.bit8 && (latch and dipSwitch != 0)) {
                return console.memoryManager.openBus()
            }
        }

        return internalRead(addr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        latch = addr

        if (latch and 0x2000 != 0) {
            selectPrgPage2x(0, latch shr 1 and 0xFE)
        } else {
            selectPrgPage(0, latch shr 1)
            selectPrgPage(1, latch shr 1)
        }

        selectChrPage(0, value and 0x03)
        mirroringType = if (latch.bit0) HORIZONTAL else VERTICAL
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("latch", latch)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        latch = s.readInt("latch")
    }

    companion object {

        private val DPSWLUT = intArrayOf(0x0010, 0x0000, 0x0030, 0x0070, 0x00F0)
    }
}
