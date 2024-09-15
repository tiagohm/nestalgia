package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_303

class Kaiser7017(console: Console) : Mapper(console) {

    @Volatile private var prgReg = 0
    @Volatile private var mirroring = VERTICAL
    @Volatile private var irqCounter = 0
    @Volatile private var irqEnabled = false

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4020

    override val registerEndAddress = 0x5FFF

    override val allowRegisterRead = true

    override fun initialize() {
        removeRegisterRange(0x4020, 0x5FFF, READ)
        addRegisterRange(0x4030, 0x4030, READ)

        selectChrPage(0, 0)
        updateState()
    }

    override fun clock() {
        if (irqEnabled && irqCounter > 0) {
            irqCounter--

            if (irqCounter == 0) {
                irqEnabled = false
                console.cpu.setIRQSource(IRQSource.EXTERNAL)
            }
        }
    }

    private fun updateState() {
        selectPrgPage(0, prgReg)
        selectPrgPage(1, 2)
        mirroringType = mirroring
    }

    override fun readRegister(addr: Int): Int {
        val irqPending = console.cpu.hasIRQSource(IRQSource.EXTERNAL)
        console.cpu.clearIRQSource(IRQSource.EXTERNAL)
        return irqPending.toInt()
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0xFF00 == 0x4A00) {
            prgReg = (addr shr 2 and 0x03) or (addr shr 4 and 0x04)
        } else if (addr and 0xFF00 == 0x5100) {
            updateState()
        } else if (addr == 0x4020) {
            console.cpu.clearIRQSource(IRQSource.EXTERNAL)
            irqCounter = (irqCounter and 0xFF00) or value
        } else if (addr == 0x4021) {
            console.cpu.clearIRQSource(IRQSource.EXTERNAL)
            irqCounter = (irqCounter and 0xFF) or (value shl 8)
            irqEnabled = true
        } else if (addr == 0x4025) {
            mirroring = if ((value shr 3).bit0) HORIZONTAL else VERTICAL
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
        s.write("mirroring", mirroring)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")
        mirroring = s.readEnum("mirroring", VERTICAL)
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
    }
}
