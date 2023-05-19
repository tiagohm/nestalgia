package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_030

class UnRom512 : FlashSST39SF040Mapper() {

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x2000U

    override val workRamSize = 0U

    override val saveRamSize = 0U

    override val registerStartAddress: UShort = 0x8000U

    override val registerEndAddress: UShort = 0xFFFFU

    override val chrRamSize = 0x8000U

    override val hasBusConflicts
        get() = !hasBattery

    override val allowRegisterRead
        get() = hasBattery

    private var enableMirroringBit = false
    private var prgBank: UByte = 0U

    override fun init() {
        super.init()

        selectPrgPage(0U, 0U)
        selectPrgPage(1U, (-1).toUShort())

        if (mirroringType == SCREEN_A_ONLY || mirroringType == SCREEN_B_ONLY) {
            mirroringType = SCREEN_A_ONLY
            enableMirroringBit = true
        } else {
            when ((info.header.byte6 and 0x09U).toInt()) {
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
            setPpuMemoryMapping(0x2000U, 0x3FFFU, ChrMemoryType.RAM, 0x6000, MemoryAccessType.READ_WRITE)
        }

        if (hasBattery) {
            addRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.READ)
            orgPrgRom = prgRom.copyOf()
            applySaveData()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        val ipsData = IpsPatcher.createPatch(orgPrgRom, prgRom)
        s.write("ipsData", ipsData)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readUByteArray("ipsData")?.also(::applyPatch)
    }

    override fun saveBattery() {
        if (hasBattery) {
            super.saveBattery()
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (!hasBattery || addr.toInt() >= 0xC000) {
            selectPrgPage(0U, (value and 0x1FU).toUShort())
            prgBank = value and 0x1FU

            selectChrPage(0U, (value shr 5 and 0x03U).toUShort())

            if (enableMirroringBit) {
                mirroringType = if (value.bit7) SCREEN_B_ONLY else SCREEN_A_ONLY
            } else {
                flash.write((addr.toInt() and 0x3FFF) or (prgBank.toInt() shl 14), value)
            }
        }
    }
}
