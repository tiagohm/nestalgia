package br.tiagohm.nestalgia.core

open class MMC3ChrRam(
    console: Console,
    private val firstRamBank: Int,
    private val lastRamBank: Int,
    chrRamSize: Int,
) : MMC3(console) {

    override val chrRamPageSize = 0x400

    override val chrRamSize = chrRamSize * 0x400

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (page in firstRamBank..lastRamBank) {
            super.selectChrPage(slot, page - firstRamBank, ChrMemoryType.RAM)
        } else {
            super.selectChrPage(slot, page, memoryType)
        }
    }
}
