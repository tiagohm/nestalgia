package br.tiagohm.nestalgia.core

open class MMC3ChrRam(
    private val firstRamBank: UShort,
    private val lastRamBank: UShort,
    chrRamSize: UShort,
) : MMC3() {

    override val chrRamPageSize = 0x400U

    override val chrRamSize = chrRamSize * 0x400U

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        if (page in firstRamBank..lastRamBank) {
            super.selectChrPage(slot, (page - firstRamBank).toUShort(), ChrMemoryType.RAM)
        } else {
            super.selectChrPage(slot, page, memoryType)
        }
    }
}