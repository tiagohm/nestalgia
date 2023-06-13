package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_095

class Mapper095(console: Console) : Namco108(console) {

    override fun writeRegister(addr: Int, value: Int) {
        super.writeRegister(addr, value)

        if (addr.bit0) {
            val nameTable1 = registers[0] shr 5 and 0x01
            val nameTable2 = registers[1] shr 5 and 0x01

            nametables(nameTable1, nameTable1, nameTable2, nameTable2)
        }
    }
}
