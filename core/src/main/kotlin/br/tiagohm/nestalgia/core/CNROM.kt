package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_003
// https://wiki.nesdev.com/w/index.php/INES_Mapper_185

class CNROM(val enableCopyProtection: Boolean) : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val hasBusConflicts: Boolean
        get() = (info.mapperId == 3 && info.subMapperId == 2) || info.mapperId == 185

    override fun init() {
        selectPrgPage(0U, 0U)
        selectChrPage(0U, getPowerOnByte().toUShort())
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (enableCopyProtection) {
            // Submapper 0: Use heuristics - "if C AND $0F is nonzero, and if C does not equal $13: CHR is enabled"
            // Submapper 4: Enable CHR-ROM if bits 0..1 of the latch hold the value 0, otherwise disable CHR-ROM.
            // Submapper 5: Enable CHR-ROM if bits 0..1 of the latch hold the value 1, otherwise disable CHR-ROM.
            // Submapper 6: Enable CHR-ROM if bits 0..1 of the latch hold the value 2, otherwise disable CHR-ROM.
            // Submapper 7: Enable CHR-ROM if bits 0..1 of the latch hold the value 3, otherwise disable CHR-ROM.
            val latch = value.toInt() and 0x03
            val isValidAccess = info.subMapperId == 0 && value.toInt() and 0x0F != 0 && value.toInt() != 0x13 ||
                    info.subMapperId == 4 && latch == 0 ||
                    info.subMapperId == 5 && latch == 1 ||
                    info.subMapperId == 6 && latch == 2 ||
                    info.subMapperId == 7 && latch == 3

            if (isValidAccess) {
                selectChrPage(0U, 0U)
            } else {
                removePpuMemoryMapping(0x0000U, 0x1FFFU)
            }
        } else {
            selectChrPage(0U, value.toUShort())
        }
    }
}