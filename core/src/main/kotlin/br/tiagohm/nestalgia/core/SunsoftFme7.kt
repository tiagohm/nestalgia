package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_069

class SunsoftFme7(console: Console) : Mapper(console) {

    private val audio = Sunsoft5bAudio(console)
    @Volatile private var command = 0
    @Volatile private var workRamValue = 0
    @Volatile private var irqEnabled = false
    @Volatile private var irqCounterEnabled = false
    @Volatile private var irqCounter = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val workRamSize = 0x8000

    override val workRamPageSize = 0x2000

    override val saveRamSize = 0x8000

    override val saveRamPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(3, -1)

        updateWorkRam()
    }

    override fun clock() {
        if (irqCounterEnabled) {
            irqCounter = (irqCounter - 1) and 0xFFFF

            if (irqCounter == 0xFFFF) {
                if (irqEnabled) {
                    console.cpu.setIRQSource(EXTERNAL)
                }
            }
        }

        audio.clock()
    }

    private fun updateWorkRam() {
        if (workRamValue.bit6) {
            val accessType = if (workRamValue.bit7) READ_WRITE else NO_ACCESS
            addCpuMemoryMapping(0x6000, 0x7FFF, workRamValue and 0x3F, if (hasBattery) SRAM else WRAM, accessType)
        } else {
            addCpuMemoryMapping(0x6000, 0x7FFF, workRamValue and 0x3F, ROM)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE000) {
            0x8000 -> command = value and 0x0F
            0xA000 -> when (command) {
                0, 1, 2, 3, 4, 5, 6, 7 -> selectChrPage(command, value)
                8 -> {
                    workRamValue = value
                    updateWorkRam()
                }
                9, 0xA, 0xB -> selectPrgPage(command - 9, value and 0x3F)
                0xC -> when (value and 0x03) {
                    0 -> mirroringType = VERTICAL
                    1 -> mirroringType = HORIZONTAL
                    2 -> mirroringType = SCREEN_A_ONLY
                    3 -> mirroringType = SCREEN_B_ONLY
                }
                0xD -> {
                    irqEnabled = value.bit0
                    irqCounterEnabled = value.bit7
                    console.cpu.clearIRQSource(EXTERNAL)
                }
                0xE -> irqCounter = irqCounter and 0xFF00 or value
                0xF -> irqCounter = irqCounter and 0xFF or (value shl 8)
            }
            0xC000, 0xE000 -> audio.write(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("audio", audio)
        s.write("command", command)
        s.write("workRamValue", workRamValue)
        s.write("irqEnabled", irqEnabled)
        s.write("irqCounterEnabled", irqCounterEnabled)
        s.write("irqCounter", irqCounter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("audio", audio)
        command = s.readInt("command")
        workRamValue = s.readInt("workRamValue")
        irqEnabled = s.readBoolean("irqEnabled")
        irqCounterEnabled = s.readBoolean("irqCounterEnabled")
        irqCounter = s.readInt("irqCounter")
    }
}
