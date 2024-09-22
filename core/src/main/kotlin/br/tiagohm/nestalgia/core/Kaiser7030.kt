package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE
import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL
import br.tiagohm.nestalgia.core.PrgMemoryType.ROM
import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM

// https://www.nesdev.org/wiki/NES_2.0_Mapper_347

class Kaiser7030(console: Console) : Mapper(console) {

    override val prgPageSize = 0x0400

    override val chrPageSize = 0x2000

    override val workRamSize = 0x2000

    override val workRamPageSize = 0x0400

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0x9FFF

    private val prgRegs = IntArray(2)
    @Volatile private var oldMaskRom = false

    override fun initialize() {
        // Last 32 KiB mapped to CPU $8000-$FFFF
        addCpuMemoryMapping(0x8000, 0xFFFF, 0x60, ROM) // rom-offset $18000

        prgRegs[0] = 0x0F
        prgRegs[1] = 0x0F
        oldMaskRom = false

        if (data.info.hash.prgCrc32 == 0xFA4DAC91) {
            oldMaskRom = true
        }

        updateState()
    }

    private fun updateState() {
        if (oldMaskRom) {
            // As the FCEUX source code comment indicates, the actual bank order in the 128 KiB mask ROM was unknown until July 2020. Emulators expected the ROM image to be laid out like this:
            // * the first 32 KiB to contain the eight banks selected by register $8000 mapped to $7000-$7FFF;
            // * the next 64 KiB to contain the sixteen banks selected by register $9000, with the first 1 KiB mapped to CPU $6C00-$6FFF and the second 3 KiB mapped to CPU $C000-$CBFF;
            // * the final 32 KiB mapped to CPU $8000-$FFFF except where replaced by RAM and the switchable PRG-ROM bank.
            addCpuMemoryMapping(0x6000, 0x6BFF, 0, WRAM)
            addCpuMemoryMapping(0x6C00, 0x6FFF, ((prgRegs[1] and 0x0F) shl 2) + 0x20, ROM, READ_WRITE) // rom-offset $8000
            addCpuMemoryMapping(0x7000, 0x7FFF, ((prgRegs[0] and 0x07) shl 2) + 0x00, ROM, READ_WRITE) // rom-offset $0
            addCpuMemoryMapping(0xB800, 0xBFFF, 3, WRAM)
            addCpuMemoryMapping(0xC000, 0xCBFF, ((prgRegs[1] and 0x0F) shl 2) + 0x21, ROM, READ_WRITE) // rom-offset $8400
            addCpuMemoryMapping(0xCC00, 0xD7FF, 5, WRAM)
        } else {
            // The actual mask ROM layout is as follows:
            // * the first 64 KiB contain the sixteen banks selected by register $9000, with the first 3 KiB mapped to CPU $C000-$CBFF and the second 1 KiB mapped to CPU $6C00-$6FFF;
            // * the next 32 KiB contain the eight banks selected by register $8000 mapped to $7000-$7FFF;
            // * the final 32 KiB mapped to CPU $8000-$FFFF except where replaced by RAM and the switchable PRG-ROM bank.
            addCpuMemoryMapping(0x6000, 0x6BFF, 0, WRAM)
            addCpuMemoryMapping(0x6C00, 0x6FFF, ((prgRegs[1] and 0x0F) shl 2) + 0x03, ROM, READ_WRITE) // rom-offset $0C00
            addCpuMemoryMapping(0x7000, 0x7FFF, ((prgRegs[0] and 0x07) shl 2) + 0x40, ROM, READ_WRITE) // rom-offset $10000
            addCpuMemoryMapping(0xB800, 0xBFFF, 3, WRAM)
            addCpuMemoryMapping(0xC000, 0xCBFF, ((prgRegs[1] and 0x0F) shl 2) + 0x00, ROM, READ_WRITE) // rom-offset $0000
            addCpuMemoryMapping(0xCC00, 0xD7FF, 5, WRAM)
        }

        mirroringType = if (prgRegs[0].bit3) HORIZONTAL else VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        prgRegs[addr shr 12 and 0x01] = addr and 0x0F
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgRegs", prgRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("prgRegs", prgRegs)
    }
}
