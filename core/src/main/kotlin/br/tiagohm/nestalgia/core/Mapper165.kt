package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_165

class Mapper165(console: Console) : MMC3(console) {

    private val chrLatch = BooleanArray(2)
    @Volatile private var needUpdate = false

    override val chrPageSize = 0x1000

    override val chrRamSize = 0x1000

    override val chrRamPageSize = 0x1000

    override fun updateChrMapping() {
        repeat(2) {
            val page = registers[if (it == 0) (if (chrLatch[0]) 1 else 0) else if (chrLatch[1]) 4 else 2]

            if (page == 0) {
                selectChrPage(it, 0, RAM)
            } else {
                selectChrPage(it, page shr 2, ROM)
            }
        }

        needUpdate = false
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (needUpdate) {
            updateChrMapping()
        }

        // MMC2 style latch.
        when (addr and 0x2FF8) {
            0xFD0, 0xFE8 -> {
                chrLatch[addr shr 12 and 0x01] = addr.bit3
                needUpdate = true
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrLatch", chrLatch)
        s.write("needUpdate", needUpdate)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readBooleanArrayOrFill("chrLatch", chrLatch, false)
        needUpdate = s.readBoolean("needUpdate")
    }
}
