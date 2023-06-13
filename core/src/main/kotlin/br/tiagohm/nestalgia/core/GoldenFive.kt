package br.tiagohm.nestalgia.core


// https://wiki.nesdev.com/w/index.php/INES_Mapper_104

class GoldenFive(console: Console) : Mapper(console) {

    private var prgReg = 0

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(1, 0x0F)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr >= 0xC000) {
            prgReg = prgReg and 0xF0 or (value and 0x0F)
            selectPrgPage(0, prgReg)
        } else if (addr <= 0x9FFF) {
            if (value.bit3) {
                prgReg = prgReg and 0x0F or (value shl 4 and 0x70)
                selectPrgPage(0, prgReg)
                selectPrgPage(1, value shl 4 and 0x70 or 0x0F)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")
    }
}
