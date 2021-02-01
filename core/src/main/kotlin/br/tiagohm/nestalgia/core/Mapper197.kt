package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_197

@ExperimentalUnsignedTypes
class Mapper197 : MMC3() {

    override fun updateChrMapping() {
        if (chrMode.isZero) {
            selectChrPage4x(0U, (registers[0].toUInt() shl 1).toUShort())
            selectChrPage2x(2U, (registers[2].toUInt() shl 1).toUShort())
            selectChrPage2x(3U, (registers[3].toUInt() shl 1).toUShort())
        } else if (chrMode.isOne) {
            selectChrPage4x(0U, (registers[2].toUInt() shl 1).toUShort())
            selectChrPage2x(2U, (registers[0].toUInt() shl 1).toUShort())
            selectChrPage2x(3U, (registers[0].toUInt() shl 1).toUShort())
        }
    }
}