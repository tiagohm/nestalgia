package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_024

open class VRC6(console: Console) : Mapper(console) {

    private val vrcIrq = VrcIrq(console)
    private val audio = VRC6Audio(console)

    @Volatile private var bankingMode = 0
    private val chrRegisters = IntArray(8)

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override fun initialize() {
        selectPrgPage(3, -1)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updatePrgRamAccess() {
        addCpuMemoryMapping(0x6000, 0x7FFF, 0, if (hasBattery) SRAM else WRAM, if (bankingMode.bit7) READ_WRITE else NO_ACCESS)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun addPpuMapping(bank: Int, page: Int) {
        addPpuMemoryMapping(0x2000 + bank * 0x400, 0x23FF + bank * 0x400, page)
        addPpuMemoryMapping(0x3000 + bank * 0x400, 0x33FF + bank * 0x400, page)
    }

    override fun clock() {
        vrcIrq.clock()
        audio.clock()
    }

    private fun updatePpuBanking() {
        val mask = if (bankingMode.bit5) 0xFE else 0xFF
        val orMask = if (bankingMode.bit5) 1 else 0

        when (bankingMode and 0x03) {
            0 -> {
                selectChrPage(0, chrRegisters[0])
                selectChrPage(1, chrRegisters[1])
                selectChrPage(2, chrRegisters[2])
                selectChrPage(3, chrRegisters[3])
                selectChrPage(4, chrRegisters[4])
                selectChrPage(5, chrRegisters[5])
                selectChrPage(6, chrRegisters[6])
                selectChrPage(7, chrRegisters[7])
            }
            1 -> {
                selectChrPage(0, chrRegisters[0] and mask)
                selectChrPage(1, chrRegisters[0] and mask or orMask)
                selectChrPage(2, chrRegisters[1] and mask)
                selectChrPage(3, chrRegisters[1] and mask or orMask)
                selectChrPage(4, chrRegisters[2] and mask)
                selectChrPage(5, chrRegisters[2] and mask or orMask)
                selectChrPage(6, chrRegisters[3] and mask)
                selectChrPage(7, chrRegisters[3] and mask or orMask)
            }
            2, 3 -> {
                selectChrPage(0, chrRegisters[0])
                selectChrPage(1, chrRegisters[1])
                selectChrPage(2, chrRegisters[2])
                selectChrPage(3, chrRegisters[3])
                selectChrPage(4, chrRegisters[4] and mask)
                selectChrPage(5, chrRegisters[4] and mask or orMask)
                selectChrPage(6, chrRegisters[5] and mask)
                selectChrPage(7, chrRegisters[5] and mask or orMask)
            }
        }

        if (bankingMode.bit4) {
            // CHR ROM nametables.
            when (bankingMode and 0x2F) {
                0x20, 0x27 -> {
                    addPpuMapping(0, chrRegisters[6] and 0xFE)
                    addPpuMapping(1, chrRegisters[6] and 0xFE or 1)
                    addPpuMapping(2, chrRegisters[7] and 0xFE)
                    addPpuMapping(3, chrRegisters[7] and 0xFE or 1)
                }
                0x23, 0x24 -> {
                    addPpuMapping(0, chrRegisters[6] and 0xFE)
                    addPpuMapping(1, chrRegisters[7] and 0xFE)
                    addPpuMapping(2, chrRegisters[6] and 0xFE or 1)
                    addPpuMapping(3, chrRegisters[7] and 0xFE or 1)
                }
                0x28, 0x2F -> {
                    addPpuMapping(0, chrRegisters[6] and 0xFE)
                    addPpuMapping(1, chrRegisters[6] and 0xFE)
                    addPpuMapping(2, chrRegisters[7] and 0xFE)
                    addPpuMapping(3, chrRegisters[7] and 0xFE)
                }
                0x2B, 0x2C -> {
                    addPpuMapping(0, chrRegisters[6] and 0xFE or 1)
                    addPpuMapping(1, chrRegisters[7] and 0xFE or 1)
                    addPpuMapping(2, chrRegisters[6] and 0xFE or 1)
                    addPpuMapping(3, chrRegisters[7] and 0xFE or 1)
                }
                else -> when (bankingMode and 0x07) {
                    0, 6, 7 -> {
                        addPpuMapping(0, chrRegisters[6])
                        addPpuMapping(1, chrRegisters[6])
                        addPpuMapping(2, chrRegisters[7])
                        addPpuMapping(3, chrRegisters[7])
                    }
                    1, 5 -> {
                        addPpuMapping(0, chrRegisters[4])
                        addPpuMapping(1, chrRegisters[5])
                        addPpuMapping(2, chrRegisters[6])
                        addPpuMapping(3, chrRegisters[7])
                    }
                    2, 3, 4 -> {
                        addPpuMapping(0, chrRegisters[6])
                        addPpuMapping(1, chrRegisters[7])
                        addPpuMapping(2, chrRegisters[6])
                        addPpuMapping(3, chrRegisters[7])
                    }
                }
            }
        } else {
            // Regular nametables (CIRAM).
            when (bankingMode and 0x2F) {
                0x20, 0x27 -> mirroringType = VERTICAL
                0x23, 0x24 -> mirroringType = HORIZONTAL
                0x28, 0x2F -> mirroringType = SCREEN_A_ONLY
                0x2B, 0x2C -> mirroringType = SCREEN_B_ONLY
                else -> when (bankingMode and 0x07) {
                    0, 6, 7 -> {
                        nametable(0, chrRegisters[6] and 0x01)
                        nametable(1, chrRegisters[6] and 0x01)
                        nametable(2, chrRegisters[7] and 0x01)
                        nametable(3, chrRegisters[7] and 0x01)
                    }
                    1, 5 -> {
                        nametable(0, chrRegisters[4] and 0x01)
                        nametable(1, chrRegisters[5] and 0x01)
                        nametable(2, chrRegisters[6] and 0x01)
                        nametable(3, chrRegisters[7] and 0x01)
                    }
                    2, 3, 4 -> {
                        nametable(0, chrRegisters[6] and 0x01)
                        nametable(1, chrRegisters[7] and 0x01)
                        nametable(2, chrRegisters[6] and 0x01)
                        nametable(3, chrRegisters[7] and 0x01)
                    }
                }
            }
        }

        updatePrgRamAccess()
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF003) {
            0x8000, 0x8001, 0x8002, 0x8003 -> selectPrgPage2x(0, value and 0x0F shl 1)
            0x9000, 0x9001, 0x9002, 0x9003, 0xA000,
            0xA001, 0xA002, 0xB000, 0xB001, 0xB002 -> audio.write(addr, value)
            0xB003 -> {
                bankingMode = value
                updatePpuBanking()
            }
            0xC000, 0xC001, 0xC002, 0xC003 -> selectPrgPage(2, value and 0x1F)
            0xD000, 0xD001, 0xD002, 0xD003 -> {
                chrRegisters[addr and 0x03] = value
                updatePpuBanking()
            }
            0xE000, 0xE001, 0xE002, 0xE003 -> {
                chrRegisters[4 + (addr and 0x03)] = value
                updatePpuBanking()
            }
            0xF000 -> vrcIrq.reloadValue(value)
            0xF001 -> vrcIrq.controlValue(value)
            0xF002 -> vrcIrq.acknowledgeIrq()
        }
    }
}
