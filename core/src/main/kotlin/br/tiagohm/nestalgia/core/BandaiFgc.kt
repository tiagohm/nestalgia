package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.EXTERNAL
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.SRAM
import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM

// https://www.nesdev.org/wiki/Bandai_FCG

abstract class BandaiFgc(console: Console) : Mapper(console) {

    @Volatile private var irqEnabled = false
    @Volatile private var irqCounter = 0
    @Volatile private var irqReload = 0
    @Volatile private var prgPage = 0
    @Volatile private var prgBankSelect = 0
    private val chrRegs = IntArray(8)

    @JvmField @Volatile protected var standardEeprom: Eeprom24C0X? = null
    @JvmField @Volatile protected var extraEeprom: Eeprom24C0X? = null

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x400

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override val allowRegisterRead = true

    override fun initialize() {
        // Only allow reads from 0x6000 to 0x7FFF.
        removeRegisterRange(0x8000, 0xFFFF, READ)

        // Last bank.
        if (info.mapperId != 153 && prgPageCount >= 0x20)
            selectPrgPage(1, 0x1F)
        else
            selectPrgPage(1, 0x0F)
    }

    override fun saveBattery() {
        standardEeprom?.saveBattery()

        if (extraEeprom != null) {
            extraEeprom!!.saveBattery()
        } else {
            // Do not call when the extra EEPROM exists
            // (prevent unused .sav file from being created).
            super.saveBattery()
        }
    }

    override fun clock() {
        if (irqEnabled) {
            // Checking counter before decrementing seems to be the only way to get both
            // Famicom Jump II - Saikyou no 7 Nin (J) and Magical Taruruuto-kun 2 - Mahou Daibouken (J)
            // to work without glitches with the same code.

            if (irqCounter <= 0) {
                console.cpu.setIRQSource(EXTERNAL)
            }

            irqCounter--
        }
    }

    override fun readRegister(addr: Int): Int {
        var output = 0

        if (extraEeprom != null && standardEeprom != null) {
            output = (standardEeprom!!.read() and extraEeprom!!.read()) shl 4
        } else if (standardEeprom != null) {
            output = standardEeprom!!.read() shl 4
        }

        return output or console.memoryManager.openBus(0xE7)
    }

    final override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0x000F) {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 -> {
                chrRegs[addr and 0x07] = value

                if (info.mapperId == 153) {
                    prgBankSelect = 0

                    repeat(8) { prgBankSelect = prgBankSelect or (chrRegs[it] and 0x01 shl 4) }

                    selectPrgPage(0, prgPage or prgBankSelect)
                    selectPrgPage(1, 0x0F or prgBankSelect)
                } else if (!hasChrRam && info.mapperId != 157) {
                    selectChrPage(addr and 0x07, value)
                }

                if (extraEeprom != null && info.mapperId == 157 && (addr and 0x0F) <= 3) {
                    extraEeprom!!.writeScl(value shr 3 and 0x01)
                }
            }
            0x08 -> {
                prgPage = if (info.mapperId != 153 && prgPageCount >= 0x20) value and 0x1F
                else value and 0x0F

                selectPrgPage(0, prgPage or prgBankSelect)
            }
            0x09 -> {
                when (value and 0x03) {
                    0 -> mirroringType = VERTICAL
                    1 -> mirroringType = HORIZONTAL
                    2 -> mirroringType = SCREEN_A_ONLY
                    3 -> mirroringType = SCREEN_B_ONLY
                }
            }
            0x0A -> {
                irqEnabled = value.bit0

                // Wiki claims there is no reload value, however this seems to
                // be the only way to make Famicom Jump II - Saikyou no 7 Nin work properly.
                if (info.mapperId != 16 || !isNes20 || info.subMapperId == 5) {
                    // On the LZ93D50 (Submapper 5), writing to this register also
                    // copies the latch to the actual counter.
                    irqCounter = irqReload
                }

                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x0B -> {
                if (info.mapperId != 16 || !isNes20 || info.subMapperId != 4) {
                    // On the LZ93D50 (Submapper 5), these registers instead modify a latch
                    // that will only be copied to the actual counter when register $800A is written to.
                    irqReload = (irqReload and 0xFF00) or value
                } else {
                    // On the FCG-1/2 (Submapper 4), writing to these two registers directly
                    // modifies the counter itself; all such games therefore disable counting before changing the counter value.
                    irqCounter = (irqCounter and 0xFF00) or value
                }
            }
            0x0C -> {
                if (info.mapperId != 16 || !isNes20 || info.subMapperId != 4) {
                    irqReload = (irqReload and 0xFF) or (value shl 8)
                } else {
                    irqCounter = (irqCounter and 0xFF00) or value
                }
            }
            0x0D -> {
                if (info.mapperId == 153) {
                    addCpuMemoryMapping(0x6000, 0x7FFF, 0, if (hasBattery) SRAM else WRAM, if (value.bit5) READ_WRITE else NO_ACCESS)
                } else {
                    val scl = (value and 0x20) shr 5
                    val sda = (value and 0x40) shr 6
                    standardEeprom?.write(scl, sda)
                    extraEeprom?.write(scl, sda)
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqEnabled", irqEnabled)
        s.write("irqCounter", irqCounter)
        s.write("irqReload", irqReload)
        s.write("prgPage", prgPage)
        s.write("prgBankSelect", prgBankSelect)
        s.write("chrRegs", chrRegs)

        standardEeprom?.also { s.write("standardEeprom", it) }
        extraEeprom?.also { s.write("extraEeprom", it) }
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqEnabled = s.readBoolean("irqEnabled")
        irqCounter = s.readInt("irqCounter")
        irqReload = s.readInt("irqReload")
        prgPage = s.readInt("prgPage")
        prgBankSelect = s.readInt("prgBankSelect")
        s.readIntArray("chrRegs", chrRegs)

        standardEeprom?.also { s.readSnapshotable("standardEeprom", it) }
        extraEeprom?.also { s.readSnapshotable("extraEeprom", it) }
    }
}
