package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_095

class Mapper095 : Namco108() {

    override fun writeRegister(addr: UShort, value: UByte) {
        super.writeRegister(addr, value)

        if (addr.bit0) {
            val nameTable1 = (registers[0].toInt() shr 5) and 0x01
            val nameTable2 = (registers[1].toInt() shr 5) and 0x01

            setNametables(nameTable1, nameTable1, nameTable2, nameTable2)
        }
    }
}