package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_010

class MMC4 : MMC2() {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x1000

    override fun initialize() {
        initializeLeftAndRightChrPage()
        selectPrgPage(1, -1)
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (needChrUpdate) {
            selectChrPage(0, leftChrPage[leftLatch])
            selectChrPage(1, rightChrPage[rightLatch])
            needChrUpdate = false
        }

        when (addr) {
            in 0x0FD8..0x0FDF -> {
                leftLatch = 0
                needChrUpdate = true
            }
            in 0x0FE8..0x0FEF -> {
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
}
