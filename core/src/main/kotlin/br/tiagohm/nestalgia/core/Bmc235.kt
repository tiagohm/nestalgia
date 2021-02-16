package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_235

@ExperimentalUnsignedTypes
class Bmc235 : Mapper() {

    private var openBus = false

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x2000U

    override fun init() {
        selectPrgPage2x(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun reset(softReset: Boolean) {
        selectPrgPage2x(0U, 0U)
        openBus = false
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        val a = addr.toInt()

        mirroringType = when {
            a and 0x0400 != 0 -> MirroringType.SCREEN_A_ONLY
            a and 0x2000 != 0 -> MirroringType.HORIZONTAL
            else -> MirroringType.VERTICAL
        }

        val mode = when (prgPageCount) {
            64U -> 0
            128U -> 1
            256U -> 2
            else -> 3
        }

        val i = a shr 8 and 0x03
        val bank = CONFIG[mode][i][0] or (addr.loByte and 0x1FU)
        openBus = false

        when {
            CONFIG[mode][i][1].isOne -> {
                openBus = true
                removeCpuMemoryMapping(0x8000U, 0xFFFFU)
            }
            a and 0x800 != 0 -> {
                val b = ((bank.toInt() shl 1) or (a shr 12 and 0x01)).toUShort()
                selectPrgPage(0U, b)
                selectPrgPage(1U, b)
            }
            else -> {
                selectPrgPage2x(0U, (bank.toUInt() shl 1).toUShort())
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("openBus", openBus)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        openBus = s.readBoolean("openBus") ?: false

        if (openBus) {
            removeCpuMemoryMapping(0x8000U, 0xFFFFU)
        }
    }

    companion object {

        private val CONFIG = arrayOf(
            arrayOf(ubyteArrayOf(0x00U, 0U), ubyteArrayOf(0x00U, 1U), ubyteArrayOf(0x00U, 1U), ubyteArrayOf(0x00U, 1U)),
            arrayOf(ubyteArrayOf(0x00U, 0U), ubyteArrayOf(0x00U, 1U), ubyteArrayOf(0x20U, 0U), ubyteArrayOf(0x00U, 1U)),
            arrayOf(ubyteArrayOf(0x00U, 0U), ubyteArrayOf(0x00U, 1U), ubyteArrayOf(0x20U, 0U), ubyteArrayOf(0x40U, 0U)),
            arrayOf(ubyteArrayOf(0x00U, 0U), ubyteArrayOf(0x20U, 0U), ubyteArrayOf(0x40U, 0U), ubyteArrayOf(0x60U, 0U))
        )
    }
}