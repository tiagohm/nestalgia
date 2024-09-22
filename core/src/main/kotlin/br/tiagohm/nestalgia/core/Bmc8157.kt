package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_301

class Bmc8157(console: Console) : Mapper(console) {

    @Volatile private var lastAddr = 0

    override val dipSwitchCount = 1

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        updateState()
        selectChrPage(0, 0)
    }

    private fun updateState() {
        val innerPrg0 = lastAddr shr 2 and 0x07
        val innerPrg1 = (lastAddr shr 7 and 0x01) or (lastAddr shr 8 and 0x02)
        val outer128Prg = lastAddr shr 5 and 0x03
        val outer512Prg = lastAddr shr 8 and 0x01
        val baseBank = when (innerPrg1) {
            0 -> 0
            1 -> innerPrg0
            else -> 7
        }

        if (outer512Prg != 0 && mPrgSize <= 1024 * 512 && dipSwitches != 0) {
            removeCpuMemoryMapping(0x8000, 0xFFFF)
        } else {
            selectPrgPage(0, (outer512Prg shl 6) or (outer128Prg shl 3) or innerPrg0)
            selectPrgPage(1, (outer512Prg shl 6) or (outer128Prg shl 3) or baseBank)
            mirroringType = if (lastAddr.bit1) HORIZONTAL else VERTICAL
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        lastAddr = addr
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("lastAddr", lastAddr)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        lastAddr = s.readInt("lastAddr")

        updateState()
    }
}
