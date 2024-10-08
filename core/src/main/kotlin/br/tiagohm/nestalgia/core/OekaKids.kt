package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_096

class OekaKids(console: Console) : Mapper(console) {

    @Volatile private var outerChrBank = 0
    @Volatile private var innerChrBank = 0
    @Volatile private var lastAddress = 0

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

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("outerChrBank", outerChrBank)
        s.write("innerChrBank", innerChrBank)
        s.write("lastAddress", lastAddress)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        outerChrBank = s.readInt("outerChrBank")
        innerChrBank = s.readInt("innerChrBank")
        lastAddress = s.readInt("lastAddress")
    }
}
