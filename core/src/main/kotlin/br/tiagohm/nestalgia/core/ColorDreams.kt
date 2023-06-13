package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_011

open class ColorDreams(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val hasBusConflicts = true

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        // TODO: Re-add size restriction when adding an option to prevent oversized roms.
        selectPrgPage(0, value and 0x0F)
        selectChrPage(0, value shr 4 and 0x0F)
    }
}
