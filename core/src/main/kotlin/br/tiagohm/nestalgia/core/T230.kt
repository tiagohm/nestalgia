package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_529

class T230(console: Console) : Mapper(console) {

    private val vrcIrq = VrcIrq(console)

    @Volatile private var prgReg0 = 0
    @Volatile private var prgReg1 = 0
    @Volatile private var prgMode = 0

    @Volatile private var outerBank = 0

    private val hiCHRRegs = IntArray(8)
    private val loCHRRegs = IntArray(8)

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override fun initialize() {
        prgMode = powerOnByte() and 0x01
        prgReg0 = powerOnByte() and 0x1F
        prgReg1 = powerOnByte() and 0x1F

        repeat(8) {
            loCHRRegs[it] = powerOnByte() and 0x0F
            hiCHRRegs[it] = powerOnByte() and 0x1F
        }

        updateState()
    }

    override fun clock() {
        vrcIrq.clock()
    }

    private fun updateState() {
        if (mChrRamSize > 0) {
            selectChrPage8x(0, 0)
        } else {
            for (i in 0..7) {
                selectChrPage(i, loCHRRegs[i] or (hiCHRRegs[i] shl 4))
            }
        }
        if (prgMode == 0) {
            selectPrgPage(0, prgReg0 or outerBank)
            selectPrgPage(2, -2 and 0x1F or outerBank)
        } else {
            selectPrgPage(0, -2 and 0x1F or outerBank)
            selectPrgPage(2, prgReg0 or outerBank)
        }

        selectPrgPage(1, prgReg1)
        selectPrgPage(3, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val newAddr = addr and 0xF000 or (if (addr and 0x2A != 0) 0x02 else 0) or if (addr and 0x15 != 0) 0x01 else 0

        if (newAddr in 0x9000..0x9001) {
            when (value) {
                0 -> mirroringType = VERTICAL
                1 -> mirroringType = HORIZONTAL
                2 -> mirroringType = SCREEN_A_ONLY
                3 -> mirroringType = SCREEN_B_ONLY
            }
        } else if (newAddr in 0x9002..0x9003) {
            prgMode = value shr 1 and 0x01
        } else if (newAddr in 0xA000..0xA003) {
            prgReg0 = value and 0x1F shl 1
            prgReg1 = value and 0x1F shl 1 or 0x01
        } else if (newAddr in 0xB000..0xE003) {
            if (mChrRamSize > 0) {
                outerBank = value and 0x08 shl 2
            } else {
                val regNumber = ((newAddr shr 12 and 0x07) - 3 shl 1) + (newAddr shr 1 and 0x01)
                val lowBits = !newAddr.bit0

                if (lowBits) {
                    //The other reg contains the low 4 bits
                    loCHRRegs[regNumber] = value and 0x0F
                } else {
                    //One reg contains the high 5 bits
                    hiCHRRegs[regNumber] = value and 0x1F
                }
            }
        } else if (newAddr == 0xF000) {
            vrcIrq.reloadValueNibble(value, false)
        } else if (newAddr == 0xF001) {
            vrcIrq.reloadValueNibble(value, true)
        } else if (newAddr == 0xF002) {
            vrcIrq.controlValue(value)
        } else if (newAddr == 0xF003) {
            vrcIrq.acknowledgeIrq()
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("vrcIrq", vrcIrq)
        s.write("prgReg0", prgReg0)
        s.write("prgReg1", prgReg1)
        s.write("prgMode", prgMode)
        s.write("outerBank", outerBank)
        s.write("hiCHRRegs", hiCHRRegs)
        s.write("loCHRRegs", loCHRRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("vrcIrq", vrcIrq)
        prgReg0 = s.readInt("prgReg0")
        prgReg1 = s.readInt("prgReg1")
        prgMode = s.readInt("prgMode")
        outerBank = s.readInt("outerBank")
        s.readIntArray("hiCHRRegs", hiCHRRegs)
        s.readIntArray("loCHRRegs", loCHRRegs)
    }
}
