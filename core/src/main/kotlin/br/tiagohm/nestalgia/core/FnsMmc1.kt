package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.RAM
import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE
import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL
import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM
import org.slf4j.LoggerFactory

// https://www.nesdev.org/wiki/Famicom_Network_System

class FnsMmc1(console: Console) : MMC1(console) {

    override val allowRegisterRead = true

    @Volatile private var kanjiRomData = ByteArray(256 * 1024)
    @Volatile private var mirroringSelect = VERTICAL
    @Volatile private var kanjiRomPos = 0
    @Volatile private var kanjiRomBank = false
    @Volatile private var chrRamBank = 0
    @Volatile private var workRamEnable1 = true
    @Volatile private var workRamEnable2 = false

    override fun initialize() {
        super.initialize()

        removeRegisterRange(0x8000, 0xFFFF, READ)
        addRegisterRange(0x40AE, 0x40C0, READ_WRITE)
        addRegisterRange(0x5000, 0x5FFF, READ)

        try {
            Thread.currentThread().contextClassLoader
                .getResourceAsStream("[BIOS] LH5323M1 Kanji Graphic ROM (Japan).bin")
                ?.use { it.readAllBytes() }
                ?.also { kanjiRomData = it }
        } catch (e: Throwable) {
            LOG.error("failed to read firmware", e)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x6000) {
            when (addr) {
                0x40AE -> {
                    workRamEnable1 = value.bit0
                    updateState()
                }
                0x40B0 -> kanjiRomBank = value.bit0
                0x40C0 -> {
                    workRamEnable2 = value.bit0
                    chrRamBank = value and 0x08 shr 3
                    updateState()
                }
                0x40AD -> {
                    mirroringSelect = if (value.bit7) HORIZONTAL else VERTICAL
                    updateState()
                }
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun readRegister(addr: Int): Int {
        if (addr < 0x5000) {
            when (addr) {
                0x40B0 -> kanjiRomPos = 0
                0x40C0 -> return 0x80
            }

            return console.memoryManager.openBus()
        } else if (addr < 0x6000) {
            val value = kanjiRomData[(if (kanjiRomBank) 0x20000 else 0) or (addr and 0xFFF shl 5) or kanjiRomPos]
            kanjiRomPos = (kanjiRomPos + 1) and 0x1F
            return value.toInt() and 0xFF
        } else {
            return super.readRegister(addr)
        }
    }

    override fun updateState() {
        super.updateState()

        mirroringType = mirroringSelect
        addPpuMemoryMapping(0x0000, 0x1FFF, RAM, if (chrRamBank != 0) 0x2000 else 0, READ_WRITE)

        if (workRamEnable1 && workRamEnable2) {
            addCpuMemoryMapping(0x6000, 0x7FFF, WRAM, 0, READ_WRITE)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("mirroringSelect", mirroringSelect)
        s.write("kanjiRomPos", kanjiRomPos)
        s.write("kanjiRomBank", kanjiRomBank)
        s.write("chrRamBank", chrRamBank)
        s.write("workRamEnable1", workRamEnable1)
        s.write("workRamEnable2", workRamEnable2)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        mirroringSelect = s.readEnum("mirroringSelect", mirroringSelect)
        kanjiRomPos = s.readInt("kanjiRomPos")
        kanjiRomBank = s.readBoolean("kanjiRomBank")
        chrRamBank = s.readInt("chrRamBank")
        workRamEnable1 = s.readBoolean("workRamEnable1")
        workRamEnable2 = s.readBoolean("workRamEnable2")
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(FnsMmc1::class.java)
    }
}
