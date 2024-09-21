package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_166
// https://wiki.nesdev.com/w/index.php/INES_Mapper_167

class Subor166(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private val regs = IntArray(4)

    override fun initialize() {
        writeRegister(0x8000, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE000) {
            0x8000 -> regs[0] = value and 0x10
            0xA000 -> regs[1] = value and 0x1C
            0xC000 -> regs[2] = value and 0x1F
            0xE000 -> regs[3] = value and 0x1F
        }

        val outerBank = regs[0] xor regs[1] and 0x10 shl 1
        val innerBank = regs[2] xor regs[3]

        val altMode = info.mapperId == 167

        if (regs[1].bit3) {
            // 32 KiB NROM
            val bank = outerBank or innerBank and 0xFE
            selectPrgPage(0, if (altMode) bank + 1 else bank)
            selectPrgPage(1, if (altMode) bank else bank + 1)
        } else if (regs[1].bit2) {
            // 512 KiB inverted UNROM(mapper 180)
            selectPrgPage(0, 0x1F)
            selectPrgPage(1, outerBank or innerBank)
        } else {
            // 512 KiB UNROM
            selectPrgPage(0, outerBank or innerBank)
            selectPrgPage(1, if (altMode) 0x20 else 0x07)
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
