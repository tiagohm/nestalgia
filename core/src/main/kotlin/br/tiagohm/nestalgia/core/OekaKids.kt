package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_096

class OekaKids : Mapper() {

    private var outerChrBank = 0
    private var innerChrBank = 0
    private var lastAddress = 0

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x1000

    override val hasBusConflicts = true

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateChrBanks() {
        selectChrPage(0, outerChrBank or innerChrBank)
        selectChrPage(1, outerChrBank or 0x03)
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (lastAddress and 0x3000 != 0x2000 && addr and 0x3000 == 0x2000) {
            innerChrBank = addr shr 8 and 0x03
            updateChrBanks()
        }

        lastAddress = addr
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value and 0x03)
        outerChrBank = value and 0x04
        updateChrBanks()
    }
}
