package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_028

class Action53(console: Console) : Mapper(console) {

    @Volatile private var selectedReg = 0
    @Volatile private var mirroringBit = 0
    private val regs = IntArray(4)

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        regs.fill(-1)
        addRegisterRange(0x5000, 0x5FFF, WRITE)
        updateState()
    }

    private fun updateState() {
        var mirroring = regs[2] and 0x03

        if (!mirroring.bit1) {
            mirroring = mirroringBit
        }

        when (mirroring) {
            0 -> mirroringType = SCREEN_A_ONLY
            1 -> mirroringType = SCREEN_B_ONLY
            2 -> mirroringType = VERTICAL
            3 -> mirroringType = HORIZONTAL
        }

        val gameSize = regs[2] and 0x30 shr 4
        val prgSize = regs[2] and 0x08 shr 3
        val slotSelect = regs[2] and 0x04 shr 2
        val chrSelect = regs[0] and 0x03
        var prgSelect = regs[1] and 0x0F
        val outerPrgSelect = regs[3] shl 1

        selectChrPage(0, chrSelect)

        if (prgSize != 0) {
            val bank = if (slotSelect != 0) 0 else 1

            when (gameSize) {
                0 -> selectPrgPage(bank, (outerPrgSelect and 0x1FE) or (prgSelect and 0x01))
                1 -> selectPrgPage(bank, (outerPrgSelect and 0x1FC) or (prgSelect and 0x03))
                2 -> selectPrgPage(bank, (outerPrgSelect and 0x1F8) or (prgSelect and 0x07))
                3 -> selectPrgPage(bank, (outerPrgSelect and 0x1F0) or (prgSelect and 0x0F))
            }

            selectPrgPage(bank xor 0x01, (outerPrgSelect and 0x1FE) or slotSelect)
        } else {
            prgSelect = prgSelect shl 1

            selectPrgPage(0, (outerPrgSelect and OUTER_AND[gameSize]) or (prgSelect and INNER_AND[gameSize]))
            selectPrgPage(1, (outerPrgSelect and OUTER_AND[gameSize]) or (prgSelect or 0x01 and INNER_AND[gameSize]))
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0x5FFF) {
            selectedReg = value and 0x80 shr 6 or (value and 0x01)
        } else if (addr >= 0x8000) {
            if (selectedReg <= 1) {
                mirroringBit = value shr 4 and 0x01
            } else if (selectedReg == 2) {
                mirroringBit = value and 0x01
            }

            regs[selectedReg] = value

            updateState()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("selectedReg", selectedReg)
        s.write("mirroringBit", mirroringBit)
        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        selectedReg = s.readInt("selectedReg")
        mirroringBit = s.readInt("mirroringBit")
        s.readIntArrayOrFill("regs", regs, 0)
    }

    companion object {

        private val OUTER_AND = intArrayOf(0x1FE, 0x1FC, 0x1F8, 0x1F0)
        private val INNER_AND = intArrayOf(0x01, 0x03, 0x07, 0x0F)
    }
}
