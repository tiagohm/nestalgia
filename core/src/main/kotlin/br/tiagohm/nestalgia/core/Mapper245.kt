package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_245

class Mapper245(console: Console) : MMC3(console) {

    override fun updateState() {
        super.updateState()

        if (hasChrRam) {
            if (chrMode) {
                selectChrPage4x(0, 4)
                selectChrPage4x(1, 0)
            } else {
                selectChrPage4x(0, 0)
                selectChrPage4x(1, 4)
            }
        }
    }

    override fun updatePrgMapping() {
        val orValue = if (registers[0].bit1) 0x40 else 0x00
        registers[6] = registers[6] and 0x3F or orValue
        registers[7] = registers[7] and 0x3F or orValue

        val lastPageInBlock = if (prgPageCount >= 0x40) 0x3F or orValue else -1

        if (prgMode) {
            selectPrgPage(0, lastPageInBlock - 1)
            selectPrgPage(1, registers[7])
            selectPrgPage(2, registers[6])
            selectPrgPage(3, lastPageInBlock)
        } else {
            selectPrgPage(0, registers[6])
            selectPrgPage(1, registers[7])
            selectPrgPage(2, lastPageInBlock - 1)
            selectPrgPage(3, lastPageInBlock)
        }
    }
}
