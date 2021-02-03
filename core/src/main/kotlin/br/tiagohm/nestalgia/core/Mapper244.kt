package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_244

@ExperimentalUnsignedTypes
class Mapper244 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override fun init() {
        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        val v = value.toInt()

        if (value.bit3) {
            selectChrPage(0U, LUT_CHR[((v shr 4) and 0x07)][(v and 0x07)])
        } else {
            selectPrgPage(0U, LUT_PRG[((v shr 4) and 0x03)][(v and 0x03)])
        }
    }

    companion object {

        private val LUT_PRG = arrayOf(
            ushortArrayOf(0U, 1U, 2U, 3U),
            ushortArrayOf(3U, 2U, 1U, 0U),
            ushortArrayOf(0U, 2U, 1U, 3U),
            ushortArrayOf(3U, 1U, 2U, 0U),
        )

        private val LUT_CHR = arrayOf(
            ushortArrayOf(0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U),
            ushortArrayOf(0U, 2U, 1U, 3U, 4U, 6U, 5U, 7U),
            ushortArrayOf(0U, 1U, 4U, 5U, 2U, 3U, 6U, 7U),
            ushortArrayOf(0U, 4U, 1U, 5U, 2U, 6U, 3U, 7U),
            ushortArrayOf(0U, 4U, 2U, 6U, 1U, 5U, 3U, 7U),
            ushortArrayOf(0U, 2U, 4U, 6U, 1U, 3U, 5U, 7U),
            ushortArrayOf(7U, 6U, 5U, 4U, 3U, 2U, 1U, 0U),
            ushortArrayOf(7U, 6U, 5U, 4U, 3U, 2U, 1U, 0U),
        )
    }
}