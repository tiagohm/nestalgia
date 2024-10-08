package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_197

class Mapper197(console: Console) : MMC3(console) {

    override fun updateChrMapping() {
        if (chrMode) {
            selectChrPage4x(0, registers[2] shl 1)
            selectChrPage2x(2, registers[0] shl 1)
            selectChrPage2x(3, registers[0] shl 1)
        } else {
            selectChrPage4x(0, registers[0] shl 1)
            selectChrPage2x(2, registers[2] shl 1)
            selectChrPage2x(3, registers[3] shl 1)
        }
    }
}
