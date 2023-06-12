package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_232

class BF9096 : Mapper() {

    private var prgBlock = 0
    private var prgPage = 0

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 3)

        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr >= 0xC000) {
            prgPage = value and 0x03
        } else {
            prgBlock = if (info.subMapperId == 1) {
                // Aladdin Deck Enhancer variation. Swap the bits of the outer bank number.
                // But this seems to match the Pegasus 4-in-1 behavior? Wiki wrong?
                (value shr 4 and 0x01) or (value shr 2 and 0x02)
            } else {
                value shr 3 and 0x03
            }
        }

        selectPrgPage(0, prgBlock shl 2 or prgPage)
        selectPrgPage(1, prgBlock shl 2 or 3)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgBlock", prgBlock)
        s.write("prgPage", prgPage)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgBlock = s.readInt("prgBlock")
        prgPage = s.readInt("prgPage")
    }
}
