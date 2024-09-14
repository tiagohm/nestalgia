package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE
import br.tiagohm.nestalgia.core.MirroringType.*
import org.slf4j.LoggerFactory

// https://wiki.nesdev.com/w/index.php/INES_Mapper_021
// https://wiki.nesdev.com/w/index.php/INES_Mapper_022
// https://wiki.nesdev.com/w/index.php/INES_Mapper_023
// https://wiki.nesdev.com/w/index.php/INES_Mapper_025
// https://wiki.nesdev.com/w/index.php/INES_Mapper_027

class VRC24(console: Console) : Mapper(console) {

    private val vrcIrq = VrcIrq(console)
    @Volatile private var variant = VRCVariant.VRC_2A
    @Volatile private var useHeuristics = false

    @Volatile private var prgReg0 = 0
    @Volatile private var prgReg1 = 0
    @Volatile private var prgMode = 0

    private val hiCHRRegs = IntArray(8)
    private val loCHRRegs = IntArray(8)

    @Volatile private var latch = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override val allowRegisterRead = true

    override fun initialize() {
        detectVariant()

        // PRG mode only exists for VRC4+ (so keep it as 0 at all times for VRC2).
        prgMode = if (variant.ordinal >= VRCVariant.VRC_4A.ordinal) powerOnByte() and 0x01 else 0

        prgReg0 = powerOnByte() and 0x1F
        prgReg1 = powerOnByte() and 0x1F

        repeat(8) {
            loCHRRegs[it] = powerOnByte() and 0x0F
            hiCHRRegs[it] = powerOnByte() and 0x1F
        }

        updateState()

        removeRegisterRange(0, 0xFFFF, READ)

        if (!useHeuristics && variant.ordinal <= VRCVariant.VRC_2C.ordinal && mWorkRamSize == 0 && mSaveRamSize == 0) {
            addRegisterRange(0x6000, 0x7FFF, READ_WRITE)
        }
    }

    private fun detectVariant() {
        variant = when (info.mapperId) {
            22 -> VRCVariant.VRC_2A
            // Conflicts: VRC4e
            23 -> when (info.subMapperId) {
                2 -> VRCVariant.VRC_4E
                else -> VRCVariant.VRC_2B
            }
            // Conflicts: VRC2c, VRC4d
            25 -> when (info.subMapperId) {
                2 -> VRCVariant.VRC_4D
                3 -> VRCVariant.VRC_2C
                else -> VRCVariant.VRC_4B
            }
            27 -> VRCVariant.VRC_4_27
            // Conflicts: VRC4c
            else -> when (info.subMapperId) {
                2 -> VRCVariant.VRC_4C
                else -> VRCVariant.VRC_4A
            }
        }

        useHeuristics = info.subMapperId == 0 && info.mapperId != 22 && info.mapperId != 27

        LOG.info("variant={}, useHeuristics={}", variant, useHeuristics)
    }

    override fun clock() {
        if (useHeuristics && info.mapperId != 22 || variant.ordinal >= VRCVariant.VRC_4A.ordinal) {
            // Only VRC4 supports IRQs.
            vrcIrq.clock()
        }
    }

    private fun updateState() {
        repeat(8) {
            var page = loCHRRegs[it] or (hiCHRRegs[it] shl 4)

            if (variant == VRCVariant.VRC_2A) {
                // On VRC2a (mapper 022) only the high 7 bits of the CHR regs are
                // used -- the low bit is ignored.  Therefore, you effectively have to right-shift
                // the CHR page by 1 to get the actual page number.
                page = page shr 1
            }

            selectChrPage(it, page)
        }

        if (prgMode == 0) {
            selectPrgPage(0, prgReg0)
            selectPrgPage(1, prgReg1)
            selectPrgPage(2, -2)
            selectPrgPage(3, -1)
        } else {
            selectPrgPage(0, -2)
            selectPrgPage(1, prgReg1)
            selectPrgPage(2, prgReg0)
            selectPrgPage(3, -1)
        }
    }

    override fun readRegister(addr: Int): Int {
        // Microwire interface ($6000-$6FFF) (VRC2 only)
        return latch or console.memoryManager.openBus(0xFE)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            // Microwire interface ($6000-$6FFF) (VRC2 only).
            latch = value and 0x01
            return
        }

        val newAddr = translateAddress(addr) and 0xF00F

        if (newAddr in 0x8000..0x8006) {
            prgReg0 = value and 0x1F
        } else if (variant.ordinal <= VRCVariant.VRC_2C.ordinal && newAddr in 0x9000..0x9003 ||
            variant.ordinal >= VRCVariant.VRC_4A.ordinal && newAddr in 0x9000..0x9001
        ) {
            var mask = 0x03

            if (!useHeuristics && variant.ordinal <= VRCVariant.VRC_2C.ordinal) {
                // When we are certain this is a VRC2 game, only use the first bit
                // for mirroring selection.
                mask = 0x01
            }

            when (value and mask) {
                0 -> mirroringType = VERTICAL
                1 -> mirroringType = HORIZONTAL
                2 -> mirroringType = SCREEN_A_ONLY
                3 -> mirroringType = SCREEN_B_ONLY
            }
        } else if (variant.ordinal >= VRCVariant.VRC_4A.ordinal && newAddr in 0x9002..0x9003) {
            prgMode = value shr 1 and 0x01
        } else if (newAddr in 0xA000..0xA006) {
            prgReg1 = value and 0x1F
        } else if (newAddr in 0xB000..0xE006) {
            val regNumber = ((newAddr shr 12 and 0x07) - 3 shl 1) + (newAddr shr 1 and 0x01)

            if (!newAddr.bit0) {
                // The other reg contains the low 4 bits.
                loCHRRegs[regNumber] = value and 0x0F
            } else {
                // One reg contains the high 5 bits.
                hiCHRRegs[regNumber] = value and 0x1F
            }
        } else if (newAddr == 0xF000) {
            vrcIrq.reloadValueNibble(value, false)
        } else if (newAddr == 0xF001) {
            vrcIrq.reloadValueNibble(value, true)
        } else if (newAddr == 0xF002) {
            vrcIrq.controlValue(value)
        } else if (newAddr == 0xF003) {
            vrcIrq.acknowledgeIrq()
        }

        updateState()
    }

    private fun translateAddress(addr: Int): Int {
        var a0 = 0
        var a1 = 0

        if (useHeuristics) {
            when (variant) {
                VRCVariant.VRC_2C,
                VRCVariant.VRC_4B,
                VRCVariant.VRC_4D -> {
                    // Mapper 25
                    // ORing both values should make most games work.
                    // VRC2c & VRC4b (Both uses the same bits)
                    a0 = addr shr 1 and 0x01
                    a1 = addr and 0x01

                    // VRC4d
                    a0 = a0 or (addr shr 3 and 0x01)
                    a1 = a1 or (addr shr 2 and 0x01)
                }
                VRCVariant.VRC_4A,
                VRCVariant.VRC_4C -> {
                    // Mapper 21
                    // VRC4a
                    a0 = addr shr 1 and 0x01
                    a1 = addr shr 2 and 0x01

                    // VRC4c
                    a0 = a0 or (addr shr 6 and 0x01)
                    a1 = a1 or (addr shr 7 and 0x01)
                }
                VRCVariant.VRC_2B,
                VRCVariant.VRC_4E -> {
                    // Mapper 23
                    // VRC2b
                    a0 = addr and 0x01
                    a1 = addr shr 1 and 0x01

                    // VRC4e
                    a0 = a0 or (addr shr 2 and 0x01)
                    a1 = a1 or (addr shr 3 and 0x01)
                }
                else -> Unit
            }
        } else {
            when (variant) {
                VRCVariant.VRC_2A -> {
                    // Mapper 22
                    a0 = addr shr 1 and 0x01
                    a1 = addr and 0x01
                }
                VRCVariant.VRC_4_27 -> {
                    // Mapper 27
                    a0 = addr and 0x01
                    a1 = addr shr 1 and 0x01
                }
                VRCVariant.VRC_2C,
                VRCVariant.VRC_4B -> {
                    // Mapper 25
                    a0 = addr shr 1 and 0x01
                    a1 = addr and 0x01
                }
                VRCVariant.VRC_4D -> {
                    // Mapper 25
                    a0 = addr shr 3 and 0x01
                    a1 = addr shr 2 and 0x01
                }
                VRCVariant.VRC_4A -> {
                    // Mapper 21
                    a0 = addr shr 1 and 0x01
                    a1 = addr shr 2 and 0x01
                }
                VRCVariant.VRC_4C -> {
                    // Mapper 21
                    a0 = addr shr 6 and 0x01
                    a1 = addr shr 7 and 0x01
                }
                VRCVariant.VRC_2B -> {
                    // Mapper 23
                    a0 = addr and 0x01
                    a1 = addr shr 1 and 0x01
                }
                VRCVariant.VRC_4E -> {
                    // Mapper 23
                    a0 = addr shr 2 and 0x01
                    a1 = addr shr 3 and 0x01
                }
                else -> Unit
            }
        }

        return addr and 0xFF00 or (a1 shl 1) or a0
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("vrcIrq", vrcIrq)
        s.write("prgReg0", prgReg0)
        s.write("prgReg1", prgReg1)
        s.write("prgMode", prgMode)
        s.write("hiCHRRegs", hiCHRRegs)
        s.write("loCHRRegs", loCHRRegs)
        s.write("latch", latch)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("vrcIrq", vrcIrq)
        prgReg0 = s.readInt("prgReg0")
        prgReg1 = s.readInt("prgReg1")
        prgMode = s.readInt("prgMode")
        s.readIntArray("hiCHRRegs", hiCHRRegs)
        s.readIntArray("loCHRRegs", loCHRRegs)
        latch = s.readInt("latch")
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(VRC24::class.java)
    }
}
