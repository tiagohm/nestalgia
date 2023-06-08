package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_144

class Mapper144 : ColorDreams() {

    override fun writeRegister(addr: Int, value: Int) {
        // This addition means that only the ROM's least significant bit always wins bus conflicts.
        super.writeRegister(addr, value or (read(addr) and 0x01))
    }
}
