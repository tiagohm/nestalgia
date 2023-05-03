package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_199

@Suppress("NOTHING_TO_INLINE")
class Mapper199 : MMC3() {

    private val exReg = UByteArray(4)

    override val chrRamSize = 0x2000U

    override val chrRamPageSize = 0x400U

    override fun init() {
        resetExReg()

        super.init()
    }

    private fun resetExReg() {
        exReg[0] = 0xFEU
        exReg[1] = 0xFFU
        exReg[2] = 1U
        exReg[3] = 3U
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toInt() == 0x8001 && state8000.bit3) {
            exReg[state8000.toInt() and 0x03] = value
            updatePrgMapping()
            updateChrMapping()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun updateMirroring() {
        mirroringType = when (stateA000.toInt() and 0x03) {
            0 -> MirroringType.VERTICAL
            1 -> MirroringType.HORIZONTAL
            2 -> MirroringType.SCREEN_A_ONLY
            else -> MirroringType.SCREEN_B_ONLY
        }
    }

    override fun updatePrgMapping() {
        super.updatePrgMapping()
        selectPrgPage(2U, exReg[0].toUShort())
        selectPrgPage(3U, exReg[1].toUShort())
    }

    private inline fun getChrMemoryType(value: UByte) = if (value < 8U) ChrMemoryType.RAM else ChrMemoryType.ROM

    private inline fun getChrMemoryType(value: UShort) = if (value < 8U) ChrMemoryType.RAM else ChrMemoryType.ROM

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, page, getChrMemoryType(page))
        super.selectChrPage(0U, registers[0].toUShort(), getChrMemoryType(registers[0]))
        super.selectChrPage(1U, exReg[2].toUShort(), getChrMemoryType(exReg[2]))
        super.selectChrPage(2U, registers[1].toUShort(), getChrMemoryType(registers[1]))
        super.selectChrPage(3U, exReg[3].toUShort(), getChrMemoryType(exReg[3]))
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