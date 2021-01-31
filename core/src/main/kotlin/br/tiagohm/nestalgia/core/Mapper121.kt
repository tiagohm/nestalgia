package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_121

@ExperimentalUnsignedTypes
class Mapper121 : MMC3() {

    private val exRegs = UByteArray(8)

    override val allowRegisterRead = true

    override fun init() {
        super.init()

        addRegisterRange(0x5000U, 0x5FFFU)
        removeRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.READ)
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        resetExRegs()
    }

    private fun resetExRegs() {
        exRegs.fill(0U)
        exRegs[3] = 0x80U
    }

    override fun readRegister(addr: UShort) = exRegs[4]

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U) {
            // $5000-$5FFF
            exRegs[4] = LOOKUP[value.toInt() and 0x03]

            if ((addr.toInt() and 0x5180) == 0x5180) {
                // Hack for Super 3-in-1
                exRegs[3] = value
                updateState()
            }
        } else if (addr < 0xA000U) {
            // $8000-$9FFF
            when {
                (addr.toInt() and 0x03) == 0x03 -> {
                    exRegs[5] = value
                    updateExRegs()
                    super.writeRegister(0x8000U, value)
                }
                addr.loByte.bit0 -> {
                    val i = value.toInt()

                    exRegs[6] = (((i and 0x01) shl 5) or
                            ((i and 0x02) shl 3) or
                            ((i and 0x04) shl 1) or
                            ((i and 0x08) shr 1) or
                            ((i and 0x10) shr 3) or
                            ((i and 0x20) shr 5)
                            ).toUByte()

                    if (exRegs[7].isZero) {
                        updateExRegs()
                    }

                    super.writeRegister(0x8001U, value)
                }
                else -> {
                    super.writeRegister(0x8000U, value)
                }
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        val o = (exRegs[3] and 0x80U) shr 2

        super.selectPrgPage(slot, (page and 0x1FU) or o.toUShort(), memoryType)

        if ((exRegs[5] and 0x3FU).isNonZero) {
            super.selectPrgPage(1U, (exRegs[2] or o).toUShort(), memoryType)
            super.selectPrgPage(2U, (exRegs[1] or o).toUShort(), memoryType)
            super.selectPrgPage(3U, (exRegs[0] or o).toUShort(), memoryType)
        }
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        if (privatePrgSize == privateChrRomSize) {
            // Hack for Super 3-in-1
            super.selectChrPage(slot, page or ((exRegs[3] and 0x80U).toUInt() shl 1).toUShort(), memoryType)
        } else if ((slot < 4U && chrMode.isZero) || (slot >= 4U && chrMode.isOne)) {
            super.selectChrPage(slot, page or 0x100U, memoryType)
        } else {
            super.selectChrPage(slot, page, memoryType)
        }
    }

    private fun updateExRegs() {
        when ((exRegs[5] and 0x3FU).toInt()) {
            0x20,
            0x29,
            0x2B,
            0x3C,
            0x3F -> {
                exRegs[7] = 1U
                exRegs[0] = exRegs[6]
            }
            0x26 -> {
                exRegs[7] = 0U
                exRegs[0] = exRegs[6]
            }
            0x2C -> {
                exRegs[7] = 1U

                if (exRegs[6].isNonZero) {
                    exRegs[0] = exRegs[6]
                }
            }
            0x28 -> {
                exRegs[7] = 0U
                exRegs[1] = exRegs[6]
            }
            0x2A -> {
                exRegs[7] = 0U
                exRegs[2] = exRegs[6]
            }
            0x2F -> {
            }
            else -> exRegs[5] = 0U
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exRegs", exRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readUByteArray("exRegs")?.copyInto(exRegs) ?: resetExRegs()
    }

    companion object {
        private val LOOKUP = ubyteArrayOf(0x83U, 0x83U, 0x42U, 0x00U)
    }
}