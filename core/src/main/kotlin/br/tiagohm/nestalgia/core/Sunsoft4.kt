package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_068

class Sunsoft4(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x800

    private val ntRegs = IntArray(2)
    private var useChrForNametables = false
    private var prgRamEnabled = false
    private var licensingTimer = 0
    private var usingExternalRom = false
    private var externalPage = 0

    override fun initialize() {
        // Bank 0's initial state is undefined, but some roms expect it to be the first page;
        selectPrgPage(0, 0)
        selectPrgPage(1, 7)

        updateState()
    }

    private fun updateState() {
        val access = if (prgRamEnabled) READ_WRITE else NO_ACCESS
        addCpuMemoryMapping(0x6000, 0x7FFF, 0, if (hasBattery) SRAM else WRAM, access)

        if (usingExternalRom) {
            if (licensingTimer == 0) {
                removeCpuMemoryMapping(0x8000, 0xBFFF)
            } else {
                selectPrgPage(0, externalPage)
            }
        }
    }

    private fun updateNametables() {
        if (useChrForNametables) {
            repeat(4) {
                val reg = when (mirroringType) {
                    VERTICAL -> it and 0x01
                    HORIZONTAL -> it and 0x02 shr 1
                    SCREEN_A_ONLY -> 0
                    SCREEN_B_ONLY -> 1
                    // 4-screen mirroring is not supported by this mapper.
                    else -> 0
                }

                addPpuMemoryMapping(
                    0x2000 + it * 0x400,
                    0x2000 + it * 0x400 + 0x3FF,
                    DEFAULT,
                    ntRegs[reg] * 0x400,
                    if (chrRamSize > 0) READ_WRITE else READ
                )
            }
        } else {
            // Reset to default mirroring.
            mirroringType = mirroringType
        }
    }

    override fun clock() {
        if (licensingTimer > 0) {
            licensingTimer--

            if (licensingTimer == 0) {
                updateState()
            }
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (addr in 0x6000..0x7FFF) {
            licensingTimer = 1024 * 105
            updateState()
        }

        super.write(addr, value, type)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF000) {
            0x8000 -> selectChrPage(0, value)
            0x9000 -> selectChrPage(1, value)
            0xA000 -> selectChrPage(2, value)
            0xB000 -> selectChrPage(3, value)
            0xC000 -> {
                ntRegs[0] = value or 0x80
                updateNametables()
            }
            0xD000 -> {
                ntRegs[1] = value or 0x80
                updateNametables()
            }
            0xE000 -> {
                when (value and 0x03) {
                    0 -> mirroringType = VERTICAL
                    1 -> mirroringType = HORIZONTAL
                    2 -> mirroringType = SCREEN_A_ONLY
                    3 -> mirroringType = SCREEN_B_ONLY
                }

                useChrForNametables = value.bit4

                updateNametables()
            }
            0xF000 -> {
                val externalPrg = !value.bit3

                if (externalPrg && prgPageCount > 8) {
                    usingExternalRom = true
                    externalPage = 0x08 or ((value and 0x07) % (prgPageCount - 0x08))
                    selectPrgPage(0, externalPage)
                } else {
                    usingExternalRom = false
                    selectPrgPage(0, value and 0x07)
                }

                prgRamEnabled = value.bit4

                updateState()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("ntRegs", ntRegs)
        s.write("useChrForNametables", useChrForNametables)
        s.write("externalPage", externalPage)
        s.write("prgRamEnabled", prgRamEnabled)
        s.write("usingExternalRom", usingExternalRom)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("ntRegs", ntRegs)
        useChrForNametables = s.readBoolean("useChrForNametables")
        externalPage = s.readInt("externalPage")
        prgRamEnabled = s.readBoolean("prgRamEnabled")
        usingExternalRom = s.readBoolean("usingExternalRom")
    }
}
