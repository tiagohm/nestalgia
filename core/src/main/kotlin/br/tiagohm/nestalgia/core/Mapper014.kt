package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_047

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Mapper014 : MMC3() {

    private val vrcChrRegs = UByteArray(8)
    private val vrcPrgRegs = UByteArray(2)
    private var vrcMirroring: UByte = 0U
    private var mode: UByte = 0U

    override fun updateChrMapping() {
        val slotSwap = if (state8000.bit7) 4U else 0U
        val outerBank0: UShort = if (mode.bit3) 0x100U else 0U
        val outerBank1: UShort = if (mode.bit5) 0x100U else 0U
        val outerBank2: UShort = if (mode.bit7) 0x100U else 0U
        selectChrPage((0U xor slotSwap).toUShort(), outerBank0 or (registers[0].toUShort() and 0xFFFEU))
        selectChrPage((1U xor slotSwap).toUShort(), outerBank0 or registers[0].toUShort() or 1U)
        selectChrPage((2U xor slotSwap).toUShort(), outerBank0 or (registers[1].toUShort() and 0xFFFEU))
        selectChrPage((3U xor slotSwap).toUShort(), outerBank0 or registers[1].toUShort() or 1U)
        selectChrPage((4U xor slotSwap).toUShort(), outerBank1 or registers[2].toUShort())
        selectChrPage((5U xor slotSwap).toUShort(), outerBank1 or registers[3].toUShort())
        selectChrPage((6U xor slotSwap).toUShort(), outerBank2 or registers[4].toUShort())
        selectChrPage((7U xor slotSwap).toUShort(), outerBank2 or registers[5].toUShort())
    }

    private inline fun updateVrcState() {
        selectPrgPage(0U, vrcPrgRegs[0].toUShort())
        selectPrgPage(1U, vrcPrgRegs[1].toUShort())
        selectPrgPage(2U, 0xFFFEU)
        selectPrgPage(3U, 0xFFFFU)

        for (i in 0..7) {
            selectChrPage(i.toUShort(), vrcChrRegs[i].toUShort())
        }

        mirroringType = if (vrcMirroring.bit0) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toInt() == 0xA131) {
            mode = value
        }

        if (mode.bit1) {
            updateState()
            super.writeRegister(addr, value)
        } else {
            if (addr >= 0xB000U && addr <= 0xEFFFU) {
                val regNumber = ((((addr.toInt() shr 12) and 0x07) - 3) shl 1) + ((addr.toInt() shr 1) and 0x01)
                val lowBits = addr.toInt() and 0x01 == 0x00

                if (lowBits) {
                    vrcChrRegs[regNumber] = (vrcChrRegs[regNumber] and 0xF0U) or (value and 0x0FU)
                } else {
                    vrcChrRegs[regNumber] =
                        (vrcChrRegs[regNumber] and 0x0FU) or ((value.toUInt() and 0x0FU) shl 4).toUByte()
                }
            } else {
                when (addr.toInt() and 0xF003) {
                    0x8000 -> vrcPrgRegs[0] = value
                    0x9000 -> vrcMirroring = value
                    0xA000 -> vrcPrgRegs[1] = value
                }
            }

            updateVrcState()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("mode", mode)
        s.write("vrcMirroring", vrcMirroring)
        s.write("vrcChrRegs", vrcChrRegs)
        s.write("vrcPrgRegs", vrcPrgRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        mode = s.readUByte("mode") ?: 0U
        vrcMirroring = s.readUByte("vrcMirroring") ?: 0U
        s.readUByteArray("vrcChrRegs")?.copyInto(vrcChrRegs) ?: vrcChrRegs.fill(0U)
        s.readUByteArray("vrcPrgRegs")?.copyInto(vrcPrgRegs) ?: vrcPrgRegs.fill(0U)
    }
}