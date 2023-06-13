package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_219

class Mapper219(console: Console) : MMC3(console) {

    private val exRegs = IntArray(3)

    override fun initialize() {
        super.initialize()

        selectPrgPage4x(0, -4)
        selectChrPage8x(0, 0)
    }

    override fun updatePrgMapping() {}

    override fun updateChrMapping() {}

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0xA000) {
            when (addr and 0xE003) {
                0x8000 -> {
                    exRegs[0] = 0
                    exRegs[1] = value
                }
                0x8001 -> {
                    if (exRegs[0] in 0x23..0x26) {
                        val prgBank = (value and 0x20 shr 5) or (value and 0x10 shr 3) or (value and 0x08 shr 1) or (value and 0x04 shl 1)
                        selectPrgPage(0x26 - exRegs[0], prgBank)
                    }
                    when (exRegs[1]) {
                        0x08, 0x0A, 0x0E, 0x12, 0x16, 0x1A, 0x1E -> exRegs[2] = value shl 4
                        0x09 -> selectChrPage(0, exRegs[2] or (value shr 1 and 0x0E))
                        0x0B -> selectChrPage(1, exRegs[2] or (value shr 1 or 0x1))
                        0x0C, 0x0D -> selectChrPage(2, exRegs[2] or (value shr 1 and 0xE))
                        0x0F -> selectChrPage(3, exRegs[2] or (value shr 1 or 0x1))
                        0x10, 0x11 -> selectChrPage(4, exRegs[2] or (value shr 1 and 0xF))
                        0x14, 0x15 -> selectChrPage(5, exRegs[2] or (value shr 1 and 0xF))
                        0x18, 0x19 -> selectChrPage(6, exRegs[2] or (value shr 1 and 0xF))
                        0x1C, 0x1D -> selectChrPage(7, exRegs[2] or (value shr 1 and 0xF))
                    }
                }
                0x8002 -> {
                    exRegs[0] = value
                    exRegs[1] = 0
                }
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exRegs", exRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exRegs", exRegs)
    }
}
