package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_011

@ExperimentalUnsignedTypes
open class ColorDreams : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val hasBusConflicts = true

    override fun init() {
        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        // TODO: Re-add size restriction when adding an option to prevent oversized roms
        selectPrgPage(0U, (value and 0x0FU).toUShort())
        selectChrPage(0U, (value shr 4 and 0x0FU).toUShort())
    }
}