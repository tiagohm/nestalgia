package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_045

class Mapper045 : MMC3() {

    private var regIndex = 0
    private val reg = UByteArray(4)

    override val registerStartAddress: UShort = 0x8000U

    override val registerEndAddress: UShort = 0xFFFFU

    override fun init() {
        super.init()

        // Needed by Famicom Yarou Vol 1 - Game apparently writes to CHR RAM before initializing the registers
        registers[0] = 0U
        registers[1] = 2U
        registers[2] = 4U
        registers[3] = 5U
        registers[4] = 6U
        registers[5] = 7U

        updateChrMapping()
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        addRegisterRange(0x6000U, 0x7FFFU)

        regIndex = 0
        resetReg()

        updateState()
    }

    private fun resetReg() {
        reg[0] = 0U
        reg[1] = 0U
        reg[2] = 0x0FU
        reg[3] = 0U
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        super.selectChrPage(
            slot,
            if (!hasChrRam) {
                val p = page and (0xFFU shr (0x0F - (reg[2].toInt() and 0x0F))).toUShort()
                p or (reg[0].toUInt() or ((reg[2].toUInt() and 0xF0U) shl 4)).toUShort()
            } else {
                page
            },
            memoryType
        )
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        super.selectPrgPage(
            slot,
            ((page.toUInt() and (0x3FU xor (reg[3].toUInt() and 0x3FU))) or reg[1].toUInt()).toUShort(),
            memoryType
        )
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U) {
            if (!reg[3].bit6) {
                reg[regIndex] = value
                regIndex = (regIndex + 1) and 0x03
            } else {
                removeRegisterRange(0x6000U, 0x7FFFU)
            }

            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regIndex", regIndex)
        s.write("reg", reg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        regIndex = s.readInt("regIndex") ?: 0
        s.readUByteArray("reg")?.copyInto(reg) ?: resetReg()

        if (reg[3].bit6) {
            removeRegisterRange(0x6000U, 0x7FFFU)
        }
    }
}