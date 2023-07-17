package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*
import org.slf4j.LoggerFactory

// https://wiki.nesdev.com/w/index.php/INES_Mapper_019
// https://wiki.nesdev.com/w/index.php/INES_Mapper_210

class Namco163(console: Console) : Mapper(console) {

    private val audio = Namco163Audio(console)
    private var notNamco340 = false
    private var autoDetectVariant = false
    private var writeProtect = 0
    private var lowChrNtMode = false
    private var highChrNtMode = false
    private var irqCounter = 0

    private var variant = NamcoVariant.NAMCO_163

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val saveRamPageSize = 0x800

    override val allowRegisterRead = true

    override fun initialize() {
        when (info.mapperId) {
            19 -> {
                variant = NamcoVariant.NAMCO_163

                when (info.gameInfo?.board) {
                    "NAMCOT-163" -> {
                        variant = NamcoVariant.NAMCO_163
                        autoDetectVariant = false
                    }
                    "NAMCOT-175" -> {
                        variant = NamcoVariant.NAMCO_175
                        autoDetectVariant = false
                    }
                    "NAMCOT-340" -> {
                        variant = NamcoVariant.NAMCO_340
                        autoDetectVariant = false
                    }
                    else -> {
                        autoDetectVariant = true
                    }
                }
            }
            210 -> when (info.subMapperId) {
                0 -> {
                    variant = NamcoVariant.UNKNOWN
                    autoDetectVariant = true
                }
                1 -> {
                    variant = NamcoVariant.NAMCO_175
                    autoDetectVariant = false
                }
                2 -> {
                    variant = NamcoVariant.NAMCO_340
                    autoDetectVariant = false
                }
            }
        }

        LOG.info("variant={}, autoDetectVariant={}", variant, autoDetectVariant)

        addRegisterRange(0x4800, 0x5FFF, READ_WRITE)
        removeRegisterRange(0x6000, 0xFFFF, READ)

        selectPrgPage(3, -1)
        updateSaveRamAccess()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateVariant(variant: NamcoVariant) {
        if (autoDetectVariant) {
            if (!notNamco340 || variant != NamcoVariant.NAMCO_340) {
                if (this.variant != variant) {
                    LOG.info("variant changed. from={}, to={}", this.variant, variant)
                    this.variant = variant
                }
            }
        }
    }

    private fun updateSaveRamAccess() {
        val memType = if (hasBattery) SRAM else WRAM

        when (variant) {
            NamcoVariant.NAMCO_163 -> {
                val writeEnable = writeProtect.bit6

                addCpuMemoryMapping(0x6000, 0x67FF, 0, memType, if (writeEnable && !writeProtect.bit0) READ_WRITE else READ)
                addCpuMemoryMapping(0x6800, 0x6FFF, 1, memType, if (writeEnable && !writeProtect.bit1) READ_WRITE else READ)
                addCpuMemoryMapping(0x7000, 0x77FF, 2, memType, if (writeEnable && !writeProtect.bit2) READ_WRITE else READ)
                addCpuMemoryMapping(0x7800, 0x7FFF, 3, memType, if (writeEnable && !writeProtect.bit3) READ_WRITE else READ)
            }
            NamcoVariant.NAMCO_175 -> addCpuMemoryMapping(0x6000, 0x7FFF, 0, memType, if (writeProtect.bit1) READ_WRITE else READ)
            else -> addCpuMemoryMapping(0x6000, 0x7FFF, 0, memType, NO_ACCESS)
        }
    }

    override fun loadBattery() {
        if (hasBattery) {
            val data = console.batteryManager.loadBattery(".sav", mSaveRamSize + audio.internalRam.size)

            if (data.isNotEmpty()) {
                data.copyInto(saveRam, endIndex = mSaveRamSize)
                data.copyInto(audio.internalRam, 0, mSaveRamSize, mSaveRamSize + audio.internalRam.size)
            }
        }
    }

    override fun saveBattery() {
        if (hasBattery) {
            val data = IntArray(mSaveRamSize + audio.internalRam.size)
            saveRam.copyInto(data)
            audio.internalRam.copyInto(data, mSaveRamSize)
            console.batteryManager.saveBattery(".sav", data)
        }
    }

    override fun clock() {
        if ((irqCounter and 0x8000) != 0 && irqCounter and 0x7FFF != 0x7FFF) {
            irqCounter++

            if (irqCounter and 0x7FFF == 0x7FFF) {
                console.cpu.setIRQSource(EXTERNAL)
            }
        }

        if (variant == NamcoVariant.NAMCO_163) {
            audio.clock()
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (addr in 0x6000..0x7FFF) {
            notNamco340 = true

            if (variant == NamcoVariant.NAMCO_340) {
                updateVariant(NamcoVariant.UNKNOWN)
            }
        }

        super.write(addr, value, type)
    }

    override fun readRegister(addr: Int): Int {
        return when (addr and 0xF800) {
            0x4800 -> audio.read(addr)
            0x5000 -> irqCounter and 0xFF
            0x5800 -> irqCounter shr 8
            else -> super.readRegister(addr)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (val newAddr = addr and 0xF800) {
            0x4800 -> {
                updateVariant(NamcoVariant.NAMCO_163)
                audio.write(newAddr, value)
            }
            0x5000 -> {
                updateVariant(NamcoVariant.NAMCO_163)
                irqCounter = irqCounter and 0xFF00 or value
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x5800 -> {
                updateVariant(NamcoVariant.NAMCO_163)
                irqCounter = irqCounter and 0x00FF or (value shl 8)
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x8000, 0x8800, 0x9000, 0x9800 -> {
                val bankNumber = newAddr - 0x8000 shr 11

                if (!lowChrNtMode && value >= 0xE0 && variant == NamcoVariant.NAMCO_163) {
                    selectChrPage(bankNumber, value and 0x01, NAMETABLE_RAM)
                } else {
                    selectChrPage(bankNumber, value)
                }
            }
            0xA000, 0xA800, 0xB000, 0xB800 -> {
                val bankNumber = (newAddr - 0xA000 shr 11) + 4

                if (!highChrNtMode && value >= 0xE0 && variant == NamcoVariant.NAMCO_163) {
                    selectChrPage(bankNumber, value and 0x01, NAMETABLE_RAM)
                } else {
                    selectChrPage(bankNumber, value)
                }
            }
            0xC000, 0xC800, 0xD000, 0xD800 -> {
                if (newAddr >= 0xC800) {
                    updateVariant(NamcoVariant.NAMCO_163)
                } else if (variant != NamcoVariant.NAMCO_163) {
                    updateVariant(NamcoVariant.NAMCO_175)
                }

                if (variant == NamcoVariant.NAMCO_175) {
                    writeProtect = value
                    updateSaveRamAccess()
                } else {
                    val bankNumber = (newAddr - 0xC000 shr 11) + 8

                    if (value >= 0xE0) {
                        selectChrPage(bankNumber, value and 0x01, NAMETABLE_RAM)
                    } else {
                        selectChrPage(bankNumber, value)
                    }
                }
            }
            0xE000 -> {
                if (value.bit7) {
                    updateVariant(NamcoVariant.NAMCO_340)
                } else if (value.bit6 && variant != NamcoVariant.NAMCO_163) {
                    updateVariant(NamcoVariant.NAMCO_340)
                }

                selectPrgPage(0, value and 0x3F)

                if (variant == NamcoVariant.NAMCO_340) {
                    when (value and 0xC0 shr 6) {
                        0 -> mirroringType = SCREEN_A_ONLY
                        1 -> mirroringType = VERTICAL
                        2 -> mirroringType = HORIZONTAL
                        3 -> mirroringType = SCREEN_B_ONLY
                    }
                } else if (variant == NamcoVariant.NAMCO_163) {
                    audio.write(newAddr, value)
                }
            }
            0xE800 -> {
                selectPrgPage(1, value and 0x3F)

                if (variant == NamcoVariant.NAMCO_163) {
                    lowChrNtMode = value.bit6
                    highChrNtMode = value.bit7
                }
            }
            0xF000 -> selectPrgPage(2, value and 0x3F)
            0xF800 -> {
                updateVariant(NamcoVariant.NAMCO_163)

                if (variant == NamcoVariant.NAMCO_163) {
                    writeProtect = value
                    updateSaveRamAccess()
                    audio.write(newAddr, value)
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("audio", audio)
        s.write("notNamco340", notNamco340)
        s.write("autoDetectVariant", autoDetectVariant)
        s.write("writeProtect", writeProtect)
        s.write("lowChrNtMode", lowChrNtMode)
        s.write("highChrNtMode", highChrNtMode)
        s.write("irqCounter", irqCounter)
        s.write("variant", variant)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("audio", audio)
        notNamco340 = s.readBoolean("notNamco340")
        autoDetectVariant = s.readBoolean("autoDetectVariant")
        writeProtect = s.readInt("writeProtect")
        lowChrNtMode = s.readBoolean("lowChrNtMode")
        highChrNtMode = s.readBoolean("highChrNtMode")
        irqCounter = s.readInt("irqCounter")
        variant = s.readEnum("variant", NamcoVariant.NAMCO_163)
    }

    companion object {

        @JvmStatic private val LOG = LoggerFactory.getLogger(Namco163::class.java)
    }
}
