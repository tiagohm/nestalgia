package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_518

class Dance2000(console: Console) : Mapper(console) {

    @Volatile private var prgReg = 0
    @Volatile private var mode = 0
    @Volatile private var lastNt = 0

    override val allowRegisterRead = true

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x1000

    override fun initialize() {
        addRegisterRange(0x5000, 0x5FFF, MemoryAccessType.WRITE)
        removeRegisterRange(0x8000, 0xFFFF, MemoryAccessType.WRITE)
        updateState()
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (mode.bit1) {
            if (addr and 0x3000 == 0x2000) {
                val currentNametable = addr shr 11 and 0x01
                if (currentNametable != lastNt) {
                    lastNt = currentNametable
                    selectChrPage(0, lastNt)
                }
            }
        } else if (lastNt != 0) {
            lastNt = 0
            selectChrPage(0, lastNt)
        }
    }

    private fun updateState() {
        selectChrPage(0, lastNt)
        selectChrPage(1, 1)

        if (mode.bit2) {
            selectPrgPage2x(0, prgReg and 0x07 shl 1)
        } else {
            selectPrgPage(0, prgReg and 0x0F)
            selectPrgPage(1, 0)
        }

        mirroringType = if (mode.bit0) HORIZONTAL else VERTICAL
    }

    override fun readRegister(addr: Int): Int {
        return if (prgReg.bit6) console.memoryManager.openBus() else internalRead(addr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x5000) {
            prgReg = value
            updateState()
        } else if (addr == 0x5200) {
            mode = value

            if (mode.bit2) {
                updateState()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
        s.write("mode", mode)
        s.write("lastNt", lastNt)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")
        mode = s.readInt("mode")
        lastNt = s.readInt("lastNt")
    }
}
