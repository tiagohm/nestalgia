package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_487

class Mapper487(console: Console) : Mapper(console) {

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0x5FFF

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    private val regs = IntArray(2)

    override fun initialize() {
        addRegisterRange(0x8000, 0xFFFF, MemoryAccessType.WRITE)
        updateState()
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        regs.fill(0)
        updateState()
    }

    private fun updateState() {
        var prg = regs[1] and 0x1E
        var chr = (((regs[1] and 0x1E) shl 2) or (regs[0] and 0x03))

        if (regs[1].bit6) {
            // 64kb banks, use inner bank for A15
            prg = prg or ((regs[0] and 0x08) shr 3)
            chr = chr or (regs[0] and 0x04)
        } else {
            // 32kb banks, use outer bank LSB as A15
            prg = prg or (regs[1] and 0x01)
            chr = chr or ((regs[1] and 0x01) shl 2)
        }

        if (regs[1].bit5) {
            // Skip first 512kb of PRG/CHR when using 2nd set of prg/chr roms
            prg += 0x10
            chr += 0x40
        }

        selectPrgPage(0, prg)
        selectChrPage(0, chr)

        mirroringType = if (regs[1].bit7) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }

    override fun write(addr: Int, value: Int) {
        if (addr < 0x6000) {
            if (addr.bit8) {
                if (addr.bit7) {
                    regs[1] = value
                } else if (!regs[1].bit5) {
                    // NINA03-style register
                    regs[0] = value and 0x0F
                }

                updateState()
            }
        } else if (regs[1].bit5) {
            //Color dreams-style register
            regs[0] = (value and 0x01 shl 3) or (value and 0x70 shr 4)
            updateState()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
    }
}
