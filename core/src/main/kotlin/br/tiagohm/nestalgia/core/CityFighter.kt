package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_266

import br.tiagohm.nestalgia.core.MirroringType.*

class CityFighter(console: Console) : Mapper(console) {

    @Volatile private var prgReg = 0
    @Volatile private var prgMode = false
    @Volatile private var mirroring = 0
    private val chrRegs = IntArray(8)
    @Volatile private var irqEnabled = false
    @Volatile private var irqCounter = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override fun initialize() {
        updateState()
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter--

            if (irqCounter == 0) {
                console.cpu.setIRQSource(IRQSource.EXTERNAL)
            }
        }
    }

    private fun updateState() {
        selectPrgPage4x(0x8000, prgReg)

        if (!prgMode) {
            selectPrgPage(2, prgReg)
        }

        repeat(8) {
            selectChrPage(it, chrRegs[it])
        }

        mirroringType = when (mirroring) {
            0 -> VERTICAL
            1 -> HORIZONTAL
            2 -> SCREEN_A_ONLY
            3 -> SCREEN_B_ONLY
            else -> return
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF00C) {
            0x9000 -> {
                prgReg = value and 0x0C
                mirroring = value and 0x03
            }
            0x9004, 0x9008, 0x900C -> if (addr and 0x800 != 0) {
                console.memoryManager.write(0x4011, value and 0x0F shl 3)
            } else {
                prgReg = value and 0x0C
            }
            0xC000, 0xC004, 0xC008, 0xC00C -> prgMode = value.bit0
            0xD000 -> chrRegs[0] = (chrRegs[0] and 0xF0) or (value and 0x0F)
            0xD004 -> chrRegs[0] = (chrRegs[0] and 0x0F) or (value shl 4)
            0xD008 -> chrRegs[1] = (chrRegs[1] and 0xF0) or (value and 0x0F)
            0xD00C -> chrRegs[1] = (chrRegs[1] and 0x0F) or (value shl 4)
            0xA000 -> chrRegs[2] = (chrRegs[2] and 0xF0) or (value and 0x0F)
            0xA004 -> chrRegs[2] = (chrRegs[2] and 0x0F) or (value shl 4)
            0xA008 -> chrRegs[3] = (chrRegs[3] and 0xF0) or (value and 0x0F)
            0xA00C -> chrRegs[3] = (chrRegs[3] and 0x0F) or (value shl 4)
            0xB000 -> chrRegs[4] = (chrRegs[4] and 0xF0) or (value and 0x0F)
            0xB004 -> chrRegs[4] = (chrRegs[4] and 0x0F) or (value shl 4)
            0xB008 -> chrRegs[5] = (chrRegs[5] and 0xF0) or (value and 0x0F)
            0xB00C -> chrRegs[5] = (chrRegs[5] and 0x0F) or (value shl 4)
            0xE000 -> chrRegs[6] = (chrRegs[6] and 0xF0) or (value and 0x0F)
            0xE004 -> chrRegs[6] = (chrRegs[6] and 0x0F) or (value shl 4)
            0xE008 -> chrRegs[7] = (chrRegs[7] and 0xF0) or (value and 0x0F)
            0xE00C -> chrRegs[7] = (chrRegs[7] and 0x0F) or (value shl 4)
            0xF000 -> irqCounter = (irqCounter and 0x1E0) or (value and 0x0F shl 1)
            0xF004 -> irqCounter = (irqCounter and 0x1E) or (value and 0x0F shl 5)
            0xF008 -> {
                irqEnabled = value.bit1
                console.cpu.clearIRQSource(IRQSource.EXTERNAL)
            }
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
        s.write("prgMode", prgMode)
        s.write("mirroring", mirroring)
        s.write("chrRegs", chrRegs)
        s.write("irqEnabled", irqEnabled)
        s.write("irqCounter", irqCounter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")
        prgMode = s.readBoolean("prgMode")
        mirroring = s.readInt("mirroring")
        s.readIntArray("chrRegs", chrRegs)
        irqEnabled = s.readBoolean("irqEnabled")
        irqCounter = s.readInt("irqCounter")
    }
}
