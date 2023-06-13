package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_030

class UnRom512(console: Console) : FlashSST39SF040Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val workRamSize = 0

    override val saveRamSize = 0

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override val chrRamSize = 0x8000

    override val hasBusConflicts
        get() = !hasBattery

    override val allowRegisterRead
        get() = hasBattery

    private var enableMirroringBit = false
    private var prgBank = 0

    override lateinit var orgPrgRom: IntArray

    override fun initialize() {
        super.initialize()

        selectPrgPage(0, 0)
        selectPrgPage(1, -1)

        if (mirroringType == SCREEN_A_ONLY || mirroringType == SCREEN_B_ONLY) {
            mirroringType = SCREEN_A_ONLY
            enableMirroringBit = true
        } else {
            when (info.header.byte6 and 0x09) {
                0 -> mirroringType = HORIZONTAL
                1 -> mirroringType = VERTICAL
                8 -> {
                    mirroringType = SCREEN_A_ONLY
                    enableMirroringBit = true
                }
                9 -> mirroringType = FOUR_SCREENS
            }
        }

        if (mirroringType == FOUR_SCREENS && chrRam.size >= 0x8000) {
            // InfiniteNesLives four-screen mirroring variation, last 8kb of CHR RAM
            // is always mapped to 0x2000-0x3FFF (0x3EFF due to palette).
            // This "breaks" the "UNROM512_4screen_test" test ROM - was the ROM actually
            // tested on this board? Seems to contradict hardware specs.
            addPpuMemoryMapping(0x2000, 0x3FFF, ChrMemoryType.RAM, 0x6000, MemoryAccessType.READ_WRITE)
        }

        if (hasBattery) {
            addRegisterRange(0x8000, 0xFFFF, MemoryOperation.READ)
            orgPrgRom = prgRom.copyOf()
            applySaveData()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        val ipsData = IpsPatcher.create(orgPrgRom, prgRom)
        s.write("ipsData", ipsData)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("ipsData")?.also(::applyPatch)
    }

    override fun saveBattery() {
        if (hasBattery) {
            super.saveBattery()
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (!hasBattery || addr >= 0xC000) {
            selectPrgPage(0, value and 0x1F)

            prgBank = value and 0x1F

            selectChrPage(0, value shr 5 and 0x03)

            if (enableMirroringBit) {
                mirroringType = if (value.bit7) SCREEN_B_ONLY else SCREEN_A_ONLY
            } else {
                flash.write((addr and 0x3FFF) or (prgBank shl 14), value)
            }
        }
    }
}
