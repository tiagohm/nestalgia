package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_137
// https://wiki.nesdev.com/w/index.php/INES_Mapper_138
// https://wiki.nesdev.com/w/index.php/INES_Mapper_139
// https://wiki.nesdev.com/w/index.php/INES_Mapper_141

class Sachen8259(console: Console, private val variant: Int) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = if (variant == SACHEN_8259_D) 0x400 else 0x800

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0x7FFF

    private var currentReg = 0
    private val regs = IntArray(8)
    private val chrOr = IntArray(3)

    private val shift = when (variant) {
        SACHEN_8259_A -> 1
        SACHEN_8259_C -> 2
        else -> 0
    }

    init {
        when (variant) {
            SACHEN_8259_A -> {
                chrOr[0] = 1
                chrOr[1] = 0
                chrOr[2] = 1
            }
            SACHEN_8259_C -> {
                chrOr[0] = 1
                chrOr[1] = 2
                chrOr[2] = 3
            }
        }
    }

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    private fun updateState() {
        val simpleMode = regs[7].bit0

        when (regs[7] shr 1 and 0x03) {
            0 -> mirroringType = if (variant == SACHEN_8259_D) HORIZONTAL else VERTICAL
            1 -> mirroringType = if (variant == SACHEN_8259_D) VERTICAL else HORIZONTAL
            2 -> nametables(0, 1, 1, 1)
            3 -> mirroringType = SCREEN_A_ONLY
        }

        if (variant == SACHEN_8259_D && simpleMode) {
            // Enable "simple" mode. (mirroring is fixed to H, and banks become weird)
            mirroringType = HORIZONTAL
        }

        selectPrgPage(0, regs[5])

        if (variant == SACHEN_8259_D) {
            selectChrPage(0, regs[0])
            selectChrPage(1, regs[4] and 0x01 shl 4 or regs[if (simpleMode) 0 else 1])
            selectChrPage(2, regs[4] and 0x02 shl 3 or regs[if (simpleMode) 0 else 2])
            selectChrPage(3, regs[4] and 0x04 shl 2 or (regs[6] and 0x01 shl 3) or regs[if (simpleMode) 0 else 3])
            selectChrPage4x(1, -4)
        } else if (!hasChrRam) {
            val chrHigh = regs[4] shl 3
            selectChrPage(0, chrHigh or regs[0] shl shift)
            selectChrPage(1, chrHigh or regs[if (simpleMode) 0 else 1] shl shift or chrOr[0])
            selectChrPage(2, chrHigh or regs[if (simpleMode) 0 else 2] shl shift or chrOr[1])
            selectChrPage(3, chrHigh or regs[if (simpleMode) 0 else 3] shl shift or chrOr[2])
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xC101) {
            0x4100 -> currentReg = value and 0x07
            0x4101 -> {
                regs[currentReg] = value and 0x07
                updateState()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
        s.write("currentReg", currentReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
        currentReg = s.readInt("currentReg")
    }

    companion object {

        const val SACHEN_8259_A = 0
        const val SACHEN_8259_B = 1
        const val SACHEN_8259_C = 2
        const val SACHEN_8259_D = 3
    }
}
