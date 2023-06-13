package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_198

// Most likely incorrect/incomplete, but works (with minor glitches) with the 2 games marked as mapper 198 that I am aware of.
// Game 1: 吞食天地: 三国外传 (Tūnshí Tiāndì - Sānguó Wàizhuàn) (CHR RAM, but uses chr banking?, has save ram at 6000-7FFF)
//         吞食天地: 三国外传 (Tūnshí Tiāndì - Sānguó Wàizhuàn) uses ram 5000-7FFF (Add Chinese character process program in 5000-5FFF)
//        Original 1994 Hong Kong release by an unknown publisher, 640 KiB PRG-ROM.
// Game 2: 成吉思汗 (Chéngjísīhán) (CHR RAM, work ram mirrored from 5000-7FFF?, doesn't use chr banking)
//         成吉思汗 (Chéngjísīhán) could actually be using MMC3_199 in reality according to the Nesdev wiki.
// These games may actually use different mappers altogether.

class Mapper198(console: Console) : MMC3(console) {

    private val exReg = IntArray(4)

    override val workRamSize = 0x1000

    override val workRamPageSize = 0x1000

    override val chrRamPageSize = 0x400

    override val isForceWorkRamSize = true

    override fun initialize() {
        resetExReg()

        // Set 4kb of work ram at $5000, mirrored
        addCpuMemoryMapping(0x5000, 0x7FFF, 0, WRAM)

        super.initialize()

        if (saveRamSize == 0) {
            addCpuMemoryMapping(0x5000, 0x7FFF, 0, WRAM)
        }
    }

    private fun resetExReg() {
        exReg[0] = 0
        exReg[1] = 1
        exReg[2] = prgPageCount - 2
        exReg[3] = prgPageCount - 1
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x8001 && (state.reg8000 and 0x07) >= 6) {
            exReg[(state.reg8000 and 0x07) - 6] = value and 0x7F
        }

        super.writeRegister(addr, value)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, exReg[slot], memoryType)
    }

    override fun updateChrMapping() {
        if (mChrRamSize > 0 &&
            registers[0] == 0 &&
            registers[1] == 0 &&
            registers[2] == 0 &&
            registers[3] == 0 &&
            registers[4] == 0 &&
            registers[5] == 0
        ) {
            selectChrPage8x(0, 0, RAM)
        } else {
            super.updateChrMapping()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exReg", exReg) ?: resetExReg()
    }
}
