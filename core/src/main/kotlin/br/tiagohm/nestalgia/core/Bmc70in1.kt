package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_236

class Bmc70in1(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    override val dipSwitchCount = 4

    @Volatile private var bankMode = 0
    @Volatile private var outerBank = 0
    @Volatile private var prgReg = 0
    @Volatile private var chrReg = 0

    @Volatile private var useOuterBank = false

    override fun initialize() {
        useOuterBank = !hasChrRom

        selectChrPage(0, 0)
        updateState()
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        bankMode = 0
        outerBank = 0
    }

    private fun updateState() {
        when (bankMode) {
            0x00, 0x10 -> {
                selectPrgPage(0, outerBank or prgReg)
                selectPrgPage(1, outerBank or 7)
            }
            0x20 -> selectPrgPage2x(0, outerBank or prgReg and 0xFE)
            0x30 -> {
                selectPrgPage(0, outerBank or prgReg)
                selectPrgPage(1, outerBank or prgReg)
            }
        }
        if (!useOuterBank) {
            selectChrPage(0, chrReg)
        }
    }

    override fun readRegister(addr: Int): Int {
        return if (bankMode == 0x10) {
            internalRead(addr and 0xFFF0 or dipSwitches)
        } else {
            internalRead(addr)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x4000 != 0) {
            bankMode = addr and 0x30
            prgReg = addr and 0x07
        } else {
            mirroringType = if (addr.bit5) HORIZONTAL else VERTICAL

            if (useOuterBank) {
                outerBank = addr and 0x03 shl 3
            } else {
                chrReg = addr and 0x07
            }
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("bankMode", bankMode)
        s.write("outerBank", outerBank)
        s.write("prgReg", prgReg)
        s.write("chrReg", chrReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        bankMode = s.readInt("bankMode")
        outerBank = s.readInt("outerBank")
        prgReg = s.readInt("prgReg")
        chrReg = s.readInt("chrReg")
    }
}
