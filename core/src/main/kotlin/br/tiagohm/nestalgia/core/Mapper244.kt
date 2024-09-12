package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_244

class Mapper244(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (value.bit3) {
            selectChrPage(0, LUT_CHR[value shr 4 and 0x07][value and 0x07])
        } else {
            selectPrgPage(0, LUT_PRG[value shr 4 and 0x03][value and 0x03])
        }
    }

    companion object {

        private val LUT_PRG = arrayOf(
            intArrayOf(0, 1, 2, 3),
            intArrayOf(3, 2, 1, 0),
            intArrayOf(0, 2, 1, 3),
            intArrayOf(3, 1, 2, 0),
        )

        private val LUT_CHR = arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            intArrayOf(0, 2, 1, 3, 4, 6, 5, 7),
            intArrayOf(0, 1, 4, 5, 2, 3, 6, 7),
            intArrayOf(0, 4, 1, 5, 2, 6, 3, 7),
            intArrayOf(0, 4, 2, 6, 1, 5, 3, 7),
            intArrayOf(0, 2, 4, 6, 1, 3, 5, 7),
            intArrayOf(7, 6, 5, 4, 3, 2, 1, 0),
            intArrayOf(7, 6, 5, 4, 3, 2, 1, 0),
        )
    }
}
