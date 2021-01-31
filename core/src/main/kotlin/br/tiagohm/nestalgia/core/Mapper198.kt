package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_198

// Most likely incorrect/incomplete, but works (with minor glitches) with the 2 games marked as mapper 198 that I am aware of.
// Game 1: 吞食天地2  (CHR RAM, but uses chr banking?, has save ram at 6000-7FFF?)
// Game 2: Cheng Ji Si Han (ES-1110) (Ch)  (CHR RAM, work ram mirrored from 5000-7FFF?, doesn't use chr banking)
// These games may actually use different mappers.

@ExperimentalUnsignedTypes
class Mapper198 : MMC3() {

    private val exReg = UByteArray(4)

    override val workRamSize = 0x1000U

    override val workRamPageSize = 0x1000U

    override val chrRamPageSize = 0x400U

    override val isForceWorkRamSize = true

    override fun init() {
        resetExReg()

        // Set 4kb of work ram at $5000, mirrored
        setCpuMemoryMapping(0x5000U, 0x7FFFU, 0, PrgMemoryType.WRAM)

        super.init()

        if (saveRamSize == 0U) {
            setCpuMemoryMapping(0x5000U, 0x7FFFU, 0, PrgMemoryType.WRAM)
        }
    }

    private fun resetExReg() {
        exReg[0] = 0U
        exReg[1] = 1U
        exReg[2] = (prgPageCount - 2U).toUByte()
        exReg[3] = (prgPageCount - 1U).toUByte()
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toInt() == 0x8001 && (state8000 and 0x07U) >= 6U) {
            exReg[(state8000.toInt() and 0x07) - 6] = value and if (value >= 0x40U) 0x4FU else 0x3FU
        }

        super.writeRegister(addr, value)
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, exReg[slot.toInt()].toUShort(), memoryType)
    }

    override fun updateChrMapping() {
        if (privateChrRamSize > 0U &&
            registers[0].isZero &&
            registers[1].isZero &&
            registers[2].isZero &&
            registers[3].isZero &&
            registers[4].isZero &&
            registers[5].isZero
        ) {
            selectChrPage8x(0U, 0U, ChrMemoryType.RAM)
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

        s.readUByteArray("exReg")?.copyInto(exReg) ?: resetExReg()
    }
}