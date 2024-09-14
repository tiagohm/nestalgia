package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_009

open class MMC2(console: Console) : Mapper(console) {

    @JvmField @Volatile protected var leftLatch = 1
    @JvmField @Volatile protected var rightLatch = 1
    @Volatile private var prgPage = 0
    @JvmField protected val leftChrPage = IntArray(2)
    @JvmField protected val rightChrPage = IntArray(2)
    @JvmField @Volatile protected var needChrUpdate = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x1000

    protected fun initializeLeftAndRightChrPage() {
        leftChrPage[0] = powerOnByte() and 0x1F
        leftChrPage[1] = powerOnByte() and 0x1F
        rightChrPage[0] = powerOnByte() and 0x1F
        rightChrPage[1] = powerOnByte() and 0x1F
    }

    override fun initialize() {
        initializeLeftAndRightChrPage()

        selectPrgPage(1, -3)
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr shr 12) {
            REG_A000 -> {
                prgPage = value and 0x0F
                selectPrgPage(0, prgPage)
            }
            REG_B000 -> {
                leftChrPage[0] = value and 0x1F
                selectChrPage(0, leftChrPage[leftLatch])
            }
            REG_C000 -> {
                leftChrPage[1] = value and 0x1F
                selectChrPage(0, leftChrPage[leftLatch])
            }
            REG_D000 -> {
                rightChrPage[0] = value and 0x1F
                selectChrPage(1, rightChrPage[rightLatch])
            }
            REG_E000 -> {
                rightChrPage[1] = value and 0x1F
                selectChrPage(1, rightChrPage[rightLatch])
            }
            REG_F000 -> {
                mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
            }
        }
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (needChrUpdate) {
            selectChrPage(0, leftChrPage[leftLatch])
            selectChrPage(1, rightChrPage[rightLatch])
            needChrUpdate = false
        }

        when (addr) {
            0x0FD8 -> {
                leftLatch = 0
                needChrUpdate = true
            }
            0x0FE8 -> {
                leftLatch = 1
                needChrUpdate = true
            }
            in 0x1FD8..0x1FDF -> {
                rightLatch = 0
                needChrUpdate = true
            }
            in 0x1FE8..0x1FEF -> {
                rightLatch = 1
                needChrUpdate = true
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("leftLatch", leftLatch)
        s.write("rightLatch", rightLatch)
        s.write("prgPage", prgPage)
        s.write("leftChrPage", leftChrPage)
        s.write("rightChrPage", rightChrPage)
        s.write("needChrUpdate", needChrUpdate)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        leftLatch = s.readInt("leftLatch", 1)
        rightLatch = s.readInt("rightLatch", 1)
        prgPage = s.readInt("prgPage")
        s.readIntArray("leftChrPage", leftChrPage)
        s.readIntArray("rightChrPage", rightChrPage)
        needChrUpdate = s.readBoolean("needChrUpdate")
    }

    companion object {

        private const val REG_A000 = 0xA
        private const val REG_B000 = 0xB
        private const val REG_C000 = 0xC
        private const val REG_D000 = 0xD
        private const val REG_E000 = 0xE
        private const val REG_F000 = 0xF
    }
}
