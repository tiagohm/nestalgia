package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_185

class Mapper185(console: Console) : CNROM(console) {

    override val hasBusConflicts = true

    override fun writeRegister(addr: Int, value: Int) {
        // Submapper 0: Use heuristics - "if C AND $0F is nonzero, and if C does not equal $13: CHR is enabled"
        // Submapper 4: Enable CHR-ROM if bits 0..1 of the latch hold the value 0, otherwise disable CHR-ROM.
        // Submapper 5: Enable CHR-ROM if bits 0..1 of the latch hold the value 1, otherwise disable CHR-ROM.
        // Submapper 6: Enable CHR-ROM if bits 0..1 of the latch hold the value 2, otherwise disable CHR-ROM.
        // Submapper 7: Enable CHR-ROM if bits 0..1 of the latch hold the value 3, otherwise disable CHR-ROM.
        val latch = value and 0x03

        val isValidAccess = info.subMapperId == 0 && value and 0x0F != 0 && value != 0x13 ||
            info.subMapperId == 4 && latch == 0 ||
            info.subMapperId == 5 && latch == 1 ||
            info.subMapperId == 6 && latch == 2 ||
            info.subMapperId == 7 && latch == 3

        if (isValidAccess) {
            selectChrPage(0, 0)
        } else {
            removePpuMemoryMapping(0x0000, 0x1FFF)
        }
    }
}
