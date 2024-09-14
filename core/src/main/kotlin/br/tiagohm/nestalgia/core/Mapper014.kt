package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_047

class Mapper014(console: Console) : MMC3(console) {

    private val vrcChrRegs = IntArray(8)
    private val vrcPrgRegs = IntArray(2)
    @Volatile private var vrcMirroring = 0
    @Volatile private var mode = 0

    override fun updateChrMapping() {
        val slotSwap = if (state.reg8000.bit7) 4 else 0
        val outerBank0 = if (mode.bit3) 0x100 else 0
        val outerBank1 = if (mode.bit5) 0x100 else 0
        val outerBank2 = if (mode.bit7) 0x100 else 0
        selectChrPage(0 xor slotSwap, outerBank0 or (registers[0] and 0xFFFE))
        selectChrPage(1 xor slotSwap, outerBank0 or registers[0] or 1)
        selectChrPage(2 xor slotSwap, outerBank0 or (registers[1] and 0xFFFE))
        selectChrPage(3 xor slotSwap, outerBank0 or registers[1] or 1)
        selectChrPage(4 xor slotSwap, outerBank1 or registers[2])
        selectChrPage(5 xor slotSwap, outerBank1 or registers[3])
        selectChrPage(6 xor slotSwap, outerBank2 or registers[4])
        selectChrPage(7 xor slotSwap, outerBank2 or registers[5])
    }

    private fun updateVrcState() {
        selectPrgPage(0, vrcPrgRegs[0])
        selectPrgPage(1, vrcPrgRegs[1])
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)

        repeat(8) {
            selectChrPage(it, vrcChrRegs[it])
        }

        mirroringType = if (vrcMirroring.bit0) HORIZONTAL
        else VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0xA131) {
            mode = value
        }

        if (mode.bit1) {
            updateState()
            super.writeRegister(addr, value)
        } else {
            if (addr in 0xB000..0xEFFF) {
                val regNumber = (((addr shr 12 and 0x07) - 3) shl 1) + (addr shr 1 and 0x01)
                val lowBits = !addr.bit0

                if (lowBits) {
                    vrcChrRegs[regNumber] = (vrcChrRegs[regNumber] and 0xF0) or (value and 0x0F)
                } else {
                    vrcChrRegs[regNumber] = (vrcChrRegs[regNumber] and 0x0F) or (value and 0x0F shl 4)
                }
            } else {
                when (addr and 0xF003) {
                    0x8000 -> vrcPrgRegs[0] = value
                    0x9000 -> vrcMirroring = value
                    0xA000 -> vrcPrgRegs[1] = value
                }
            }

            updateVrcState()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("mode", mode)
        s.write("vrcMirroring", vrcMirroring)
        s.write("vrcChrRegs", vrcChrRegs)
        s.write("vrcPrgRegs", vrcPrgRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        mode = s.readInt("mode")
        vrcMirroring = s.readInt("vrcMirroring")
        s.readIntArrayOrFill("vrcChrRegs", vrcChrRegs, 0)
        s.readIntArrayOrFill("vrcPrgRegs", vrcPrgRegs, 0)
    }
}
