package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_114

@ExperimentalUnsignedTypes
class Mapper114 : MMC3() {

    private val exRegs = UByteArray(2)

    override val registerStartAddress: UShort = 0x5000U

    override val forceMmc3RevAIrqs = true

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        exRegs[0] = 0U
        exRegs[1] = 0U
    }

    override fun updatePrgMapping() {
        super.updatePrgMapping()

        if (exRegs[0].bit7) {
            val page = ((exRegs[0] and 0x0FU).toUInt() shl 1).toUShort()
            selectPrgPage2x(0U, page)
            selectPrgPage2x(1U, page)
        } else {
            super.updatePrgMapping()
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U) {
            exRegs[0] = value
            updatePrgMapping()
        } else {
            when (addr.toInt() and 0xE001) {
                0x8001 -> super.writeRegister(0xA000U, value)
                0xA000 -> {
                    super.writeRegister(0x8000U, (value and 0xC0U) or SECURITY[value.toInt() and 0x07])
                    exRegs[1] = 1U
                }
                0xA001 -> irqReloadValue = value
                0xC000 -> {
                    if (exRegs[1].isNonZero) {
                        exRegs[1] = 0U
                        super.writeRegister(0x8001U, value)
                    }
                }
                0xC001 -> irqReload = true
                0xE000 -> {
                    console.cpu.clearIRQSource(IRQSource.EXTERNAL)
                    irqEnabled = false
                }
                0xE001 -> irqEnabled = true
            }
        }


    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exRegs0", exRegs[0])
        s.write("exRegs1", exRegs[1])
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        exRegs[0] = s.readUByte("exRegs0") ?: 0U
        exRegs[1] = s.readUByte("exRegs1") ?: 0U
    }

    companion object {

        private val SECURITY = ubyteArrayOf(0U, 3U, 1U, 5U, 6U, 7U, 2U, 4U)
    }
}