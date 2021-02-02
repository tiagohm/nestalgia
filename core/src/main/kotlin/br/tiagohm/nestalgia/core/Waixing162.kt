package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_162

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Waixing162 : Mapper() {

    private val exReg = UByteArray(4)

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x5000U

    override val registerEndAddress: UShort = 0x5FFFU

    override fun init() {
        resetExReg()
        selectChrPage(0U, 0U)
        updateState()
    }

    private fun resetExReg() {
        exReg[0] = 3U
        exReg[1] = 0U
        exReg[2] = 0U
        exReg[3] = 7U
    }

    private inline fun updateState() {
        when (exReg[3].toInt() and 0x05) {
            0 -> {
                val page = (exReg[0] and 0x0CU) or
                        (exReg[1] and 0x02U) or
                        ((exReg[2] and 0x0FU).toUInt() shl 4).toUByte()
                selectPrgPage(0U, page.toUShort())
            }
            1 -> {
                val page = (exReg[0] and 0x0CU) or ((exReg[2] and 0x0FU).toUInt() shl 4).toUByte()
                selectPrgPage(0U, page.toUShort())
            }
            4 -> {
                val page = (exReg[0] and 0x0EU) or
                        ((exReg[1] shr 1) and 0x01U) or
                        ((exReg[2].toUInt() and 0x0FU) shl 4).toUByte()
                selectPrgPage(0U, page.toUShort())
            }
            5 -> {
                val page = (exReg[0] and 0x0FU) or ((exReg[2] and 0x0FU).toUInt() shl 4).toUByte()
                selectPrgPage(0U, page.toUShort())
            }
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        exReg[(addr.toInt() shr 8) and 0x03] = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readUByteArray("exReg")?.copyInto(exReg) ?: resetExReg()
    }
}